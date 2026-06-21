use serde::{Deserialize, Serialize};
use serde_json::{Map, Value};

/// HTTP request body accepted by execute and resolve endpoints.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ExecutionRequestDto {
    #[serde(rename = "type")]
    pub request_type: String,
    #[serde(rename = "toolCode", skip_serializing_if = "Option::is_none")]
    pub tool_code: Option<String>,
    #[serde(rename = "implementationCode", skip_serializing_if = "Option::is_none")]
    pub implementation_code: Option<String>,
    #[serde(default, skip_serializing_if = "Map::is_empty")]
    pub input: Map<String, Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prompt: Option<String>,
    #[serde(default, skip_serializing_if = "Map::is_empty")]
    pub payload: Map<String, Value>,
    #[serde(default, skip_serializing_if = "Map::is_empty")]
    pub attributes: Map<String, Value>,
    pub actor: ActorDto,
    pub source: SourceDto,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub correlation: Option<CorrelationDto>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<VersionDto>,
    #[serde(default, skip_serializing_if = "Map::is_empty")]
    pub metadata: Map<String, Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ActorDto {
    pub id: String,
    #[serde(rename = "type")]
    pub actor_type: String,
    #[serde(rename = "displayName", skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SourceDto {
    #[serde(rename = "type")]
    pub source_type: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    #[serde(default, skip_serializing_if = "Map::is_empty")]
    pub metadata: Map<String, Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CorrelationDto {
    #[serde(rename = "executionId", skip_serializing_if = "Option::is_none")]
    pub execution_id: Option<String>,
    #[serde(rename = "correlationId", skip_serializing_if = "Option::is_none")]
    pub correlation_id: Option<String>,
    #[serde(rename = "causationId", skip_serializing_if = "Option::is_none")]
    pub causation_id: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct VersionDto {
    #[serde(rename = "definitionVersion", skip_serializing_if = "Option::is_none")]
    pub definition_version: Option<String>,
    #[serde(rename = "implementationVersion", skip_serializing_if = "Option::is_none")]
    pub implementation_version: Option<String>,
    #[serde(default, skip_serializing_if = "Map::is_empty")]
    pub metadata: Map<String, Value>,
}

/// Minimal descriptor shape used by `run21ctl tools`.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ToolDescriptorDto {
    pub code: String,
    pub name: String,
    pub description: String,
    pub status: String,
    #[serde(rename = "inputSchema")]
    pub input_schema: Map<String, Value>,
    #[serde(rename = "outputSchema")]
    pub output_schema: Map<String, Value>,
}
