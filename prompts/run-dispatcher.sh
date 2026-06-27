#!/usr/bin/env bash
set -euo pipefail

MODEL="${MODEL:-qwen2.5:1.5b}"
OLLAMA_URL="${OLLAMA_URL:-http://localhost:11434/api/chat}"

if [[ $# -eq 0 ]]; then
  echo "Usage:"
  echo "  $0 \"Prepare a formula.evaluate request for expression a + b with variables a = 10 and b = 11.\""
  echo
  echo "Optional env vars:"
  echo "  MODEL=qwen2.5:1.5b"
  echo "  OLLAMA_URL=http://localhost:11434/api/chat"
  exit 1
fi

USER_PROMPT="$*"

SYSTEM_PROMPT=$(cat <<'EOF'
You are a strict JSON dispatcher.

Return exactly one JSON object matching the schema.
Return JSON only.
Do not explain.
Do not execute tools.
Do not calculate formula results.

The only supported tool is formula.evaluate.
The only supported implementation is formula-evalex-v1.

Accept only when the user explicitly asks for formula.evaluate or clearly asks to prepare a formula evaluation request.

Reject when:
- the user asks for any unsupported tool
- the user asks for weather, search, email, calendar, web, database, file, code execution, or anything other than formula.evaluate
- expression is missing
- the request is ambiguous
- expression uses a variable and the user did not provide its value

For accepted formula.evaluate requests:
- decision must be accept
- targetTool must be formula.evaluate
- targetImplementation must be formula-evalex-v1
- confidence must be 1.0
- message must be null
- details must be {}
- metadata must be {}
- put the formula expression into inputPayload.expression
- inputPayload.variables is always required
- put provided variables into inputPayload.variables
- if the user provides no variables, return inputPayload.variables as {}
- never omit inputPayload.variables
- never invent variables
- never invent missing variable values
- never copy a value from one variable to another variable
- never invent scale
- never invent roundingMode

For rejected requests:
- decision must be reject
- targetTool must be null
- targetImplementation must be null
- inputPayload must be {}
- confidence must be 0.0
- message must be null
- details.reason must contain the rejection reason
- details.missingVariables must contain all missing variable names when variables are missing
- details.missingVariables must be [] when the rejection reason is not missing_variables
- metadata must be {}

Allowed rejection reasons:
- missing_variables
- unsupported_tool
- missing_expression
- ambiguous_request
- invalid_model_response

Important:
Never convert an unsupported request into a formula expression.
Never convert weather.lookup, search, email, calendar, or web requests into formula.evaluate.
If expression is "a + b" and the user provides only "a = 10", reject the request.
If a variable value is missing, reject the request.
Do not guess missing variable values.

Example accepted request:

User:
Prepare a formula.evaluate request for expression a + b with variables a = 10 and b = 11.

Output:
{
  "decision": "accept",
  "targetTool": "formula.evaluate",
  "targetImplementation": "formula-evalex-v1",
  "inputPayload": {
    "expression": "a + b",
    "variables": {
      "a": 10,
      "b": 11
    }
  },
  "confidence": 1.0,
  "message": null,
  "details": {},
  "metadata": {}
}

Example accepted request without variables:

User:
Prepare a formula.evaluate request for expression 10 + 11.

Output:
{
  "decision": "accept",
  "targetTool": "formula.evaluate",
  "targetImplementation": "formula-evalex-v1",
  "inputPayload": {
    "expression": "10 + 11",
    "variables": {}
  },
  "confidence": 1.0,
  "message": null,
  "details": {},
  "metadata": {}
}

Example rejected request with unsupported tool:

User:
Prepare a weather.lookup request for Tbilisi tomorrow.

Output:
{
  "decision": "reject",
  "targetTool": null,
  "targetImplementation": null,
  "inputPayload": {},
  "confidence": 0.0,
  "message": null,
  "details": {
    "reason": "unsupported_tool",
    "missingVariables": []
  },
  "metadata": {}
}

Example rejected request with missing variable:

User:
Prepare a formula.evaluate request for expression a + b with variable a = 10.

Output:
{
  "decision": "reject",
  "targetTool": null,
  "targetImplementation": null,
  "inputPayload": {},
  "confidence": 0.0,
  "message": null,
  "details": {
    "reason": "missing_variables",
    "missingVariables": ["b"]
  },
  "metadata": {}
}
EOF
)

SCHEMA=$(cat <<'EOF'
{
  "oneOf": [
    {
      "type": "object",
      "properties": {
        "decision": {
          "type": "string",
          "enum": ["accept"]
        },
        "targetTool": {
          "type": "string",
          "enum": ["formula.evaluate"]
        },
        "targetImplementation": {
          "type": "string",
          "enum": ["formula-evalex-v1"]
        },
        "inputPayload": {
          "type": "object",
          "properties": {
            "expression": {
              "type": "string"
            },
            "variables": {
              "type": "object",
              "additionalProperties": {
                "type": ["number", "string", "boolean"]
              }
            }
          },
          "required": ["expression", "variables"],
          "additionalProperties": false
        },
        "confidence": {
          "type": "number"
        },
        "message": {
          "type": ["string", "null"]
        },
        "details": {
          "type": "object"
        },
        "metadata": {
          "type": "object"
        }
      },
      "required": [
        "decision",
        "targetTool",
        "targetImplementation",
        "inputPayload",
        "confidence",
        "message",
        "details",
        "metadata"
      ],
      "additionalProperties": false
    },
    {
      "type": "object",
      "properties": {
        "decision": {
          "type": "string",
          "enum": ["reject"]
        },
        "targetTool": {
          "type": "null"
        },
        "targetImplementation": {
          "type": "null"
        },
        "inputPayload": {
          "type": "object"
        },
        "confidence": {
          "type": "number",
          "enum": [0.0]
        },
        "message": {
          "type": ["string", "null"]
        },
        "details": {
          "type": "object",
          "properties": {
            "reason": {
              "type": "string",
              "enum": [
                "missing_variables",
                "unsupported_tool",
                "missing_expression",
                "ambiguous_request",
                "invalid_model_response"
              ]
            },
            "missingVariables": {
              "type": "array",
              "items": {
                "type": "string"
              }
            }
          },
          "required": ["reason", "missingVariables"],
          "additionalProperties": false
        },
        "metadata": {
          "type": "object"
        }
      },
      "required": [
        "decision",
        "targetTool",
        "targetImplementation",
        "inputPayload",
        "confidence",
        "message",
        "details",
        "metadata"
      ],
      "additionalProperties": false
    }
  ]
}
EOF
)

VALIDATOR_FILE="$(mktemp)"
trap 'rm -f "$VALIDATOR_FILE"' EXIT

cat > "$VALIDATOR_FILE" <<'PY'
import json
import os
import re
import sys

user_prompt = os.environ.get("USER_PROMPT_FOR_VALIDATION", "")
raw_response = os.environ.get("MODEL_RESPONSE_FOR_VALIDATION", "")

IGNORED_EXPRESSION_NAMES = {
    "true", "false", "null",
    "pi", "PI", "e", "E",
    "sin", "cos", "tan",
    "asin", "acos", "atan",
    "sqrt", "abs",
    "log", "ln", "exp",
    "pow", "min", "max",
    "round", "floor", "ceil"
}

IGNORED_PROMPT_ASSIGNMENT_NAMES = {
    "expression",
    "variable",
    "variables",
    "scale",
    "roundingMode"
}

def reject(reason, missing_variables=None):
    return {
        "decision": "reject",
        "targetTool": None,
        "targetImplementation": None,
        "inputPayload": {},
        "confidence": 0.0,
        "message": None,
        "details": {
            "reason": reason,
            "missingVariables": sorted(missing_variables or [])
        },
        "metadata": {}
    }

def accept(expression, variables):
    return {
        "decision": "accept",
        "targetTool": "formula.evaluate",
        "targetImplementation": "formula-evalex-v1",
        "inputPayload": {
            "expression": expression,
            "variables": variables
        },
        "confidence": 1.0,
        "message": None,
        "details": {},
        "metadata": {}
    }

def extract_expression_variable_names(expression):
    names = set(re.findall(r"\b[a-zA-Z_][a-zA-Z0-9_]*\b", expression))
    return names - IGNORED_EXPRESSION_NAMES

def extract_prompt_variable_names(prompt):
    names = set()

    assignment_patterns = [
        r"\b([a-zA-Z_][a-zA-Z0-9_]*)\s*=",
        r"\b([a-zA-Z_][a-zA-Z0-9_]*)\s*:",
        r"\b([a-zA-Z_][a-zA-Z0-9_]*)\s+is\s+[-+]?\d+(?:\.\d+)?\b"
    ]

    for pattern in assignment_patterns:
        for match in re.findall(pattern, prompt):
            if match not in IGNORED_PROMPT_ASSIGNMENT_NAMES:
                names.add(match)

    return names

try:
    response = json.loads(raw_response)
except json.JSONDecodeError:
    print(json.dumps(reject("invalid_model_response"), indent=2))
    sys.exit(0)

if response.get("decision") == "reject":
    details = response.get("details")
    if not isinstance(details, dict):
        print(json.dumps(reject("invalid_model_response"), indent=2))
        sys.exit(0)

    reason = details.get("reason")
    missing_variables = details.get("missingVariables")

    if reason not in {
        "missing_variables",
        "unsupported_tool",
        "missing_expression",
        "ambiguous_request",
        "invalid_model_response"
    }:
        print(json.dumps(reject("invalid_model_response"), indent=2))
        sys.exit(0)

    if not isinstance(missing_variables, list):
        print(json.dumps(reject("invalid_model_response"), indent=2))
        sys.exit(0)

    print(json.dumps(reject(reason, missing_variables), indent=2))
    sys.exit(0)

if response.get("decision") != "accept":
    print(json.dumps(reject("invalid_model_response"), indent=2))
    sys.exit(0)

payload = response.get("inputPayload")
if not isinstance(payload, dict):
    print(json.dumps(reject("invalid_model_response"), indent=2))
    sys.exit(0)

expression = payload.get("expression")
variables = payload.get("variables")

if not isinstance(expression, str) or not expression.strip():
    print(json.dumps(reject("missing_expression"), indent=2))
    sys.exit(0)

if variables is None:
    variables = {}

if not isinstance(variables, dict):
    print(json.dumps(reject("invalid_model_response"), indent=2))
    sys.exit(0)

required_variables = extract_expression_variable_names(expression)
provided_variables_in_prompt = extract_prompt_variable_names(user_prompt)

missing_variables = required_variables - provided_variables_in_prompt

if missing_variables:
    print(json.dumps(reject("missing_variables", missing_variables), indent=2))
    sys.exit(0)

unexpected_returned_variables = set(variables.keys()) - provided_variables_in_prompt

if unexpected_returned_variables:
    print(json.dumps(reject("invalid_model_response"), indent=2))
    sys.exit(0)

print(json.dumps(accept(expression, variables), indent=2))
PY

REQUEST_JSON=$(
  jq -n \
    --arg model "$MODEL" \
    --arg systemPrompt "$SYSTEM_PROMPT" \
    --arg userPrompt "$USER_PROMPT" \
    --argjson schema "$SCHEMA" \
    '{
      model: $model,
      stream: false,
      format: $schema,
      options: {
        temperature: 0
      },
      messages: [
        {
          role: "system",
          content: $systemPrompt
        },
        {
          role: "user",
          content: $userPrompt
        }
      ]
    }'
)

MODEL_RESPONSE=$(
  curl -sS "$OLLAMA_URL" \
    -H "Content-Type: application/json" \
    -d "$REQUEST_JSON" \
    | jq -r '.message.content // empty'
)

USER_PROMPT_FOR_VALIDATION="$USER_PROMPT" \
MODEL_RESPONSE_FOR_VALIDATION="$MODEL_RESPONSE" \
python3 "$VALIDATOR_FILE"
