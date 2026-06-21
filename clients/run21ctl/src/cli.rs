use anyhow::Result;
use clap::{Args, Parser, Subcommand};
use serde_json::Map;

use crate::dto::{ActorDto, CorrelationDto, ExecutionRequestDto, SourceDto};

/// Command-line interface for calling the Run21AI REST API.
#[derive(Debug, Parser)]
#[command(name = "run21ctl")]
#[command(about = "CLI client for the Run21AI REST API")]
pub struct Cli {
    /// Base URL of the Run21AI REST API.
    #[arg(long, global = true, env = "RUN21_BASE_URL", default_value = "http://127.0.0.1:8080")]
    pub base_url: String,

    #[command(subcommand)]
    pub command: Command,
}

/// Top-level commands supported by the REST API client.
#[derive(Debug, Subcommand)]
pub enum Command {
    /// Check backend liveness.
    Health,
    /// Check backend readiness.
    Ready,
    /// List available tool descriptors.
    Tools,
    /// Get one tool descriptor by code.
    Tool {
        /// Tool code, for example formula.evaluate.
        code: String,
    },
    /// Execute a structured or prompt-like request.
    Execute(RequestArgs),
    /// Resolve a request through the dispatcher chain without executing it.
    Resolve(RequestArgs),
}

/// Shared request arguments for execute and resolve commands.
#[derive(Debug, Args)]
pub struct RequestArgs {
    /// Target tool code for structured requests.
    #[arg(long)]
    pub tool_code: Option<String>,
    /// Optional implementation code for structured requests.
    #[arg(long)]
    pub implementation_code: Option<String>,
    /// Structured input payload as a JSON object.
    #[arg(long)]
    pub input_json: Option<String>,
    /// Prompt text for raw dispatcher-chain requests.
    #[arg(long)]
    pub prompt: Option<String>,
    /// Additional raw payload as a JSON object.
    #[arg(long)]
    pub payload_json: Option<String>,
    /// Dispatcher attributes as a JSON object.
    #[arg(long)]
    pub attributes_json: Option<String>,
    /// Actor id recorded in request audit metadata.
    #[arg(long, default_value = "run21ctl-user")]
    pub actor_id: String,
    /// Actor type sent to the REST API.
    #[arg(long, default_value = "human")]
    pub actor_type: String,
    /// Optional actor display name.
    #[arg(long)]
    pub actor_name: Option<String>,
    /// Execution source type sent to the REST API.
    #[arg(long, default_value = "cli")]
    pub source_type: String,
    /// Execution source name sent to the REST API.
    #[arg(long, default_value = "run21ctl")]
    pub source_name: Option<String>,
    /// Optional correlation id for audit metadata.
    #[arg(long)]
    pub correlation_id: Option<String>,
}

impl RequestArgs {
    /// Converts CLI flags into the shared REST request body.
    pub fn into_request(self) -> Result<ExecutionRequestDto> {
        let request_type = if self.prompt.is_some() { "prompt" } else { "structured" };
        let input = parse_json_object(self.input_json)?;
        let payload = parse_json_object(self.payload_json)?;
        let attributes = parse_json_object(self.attributes_json)?;

        Ok(ExecutionRequestDto {
            request_type: request_type.to_string(),
            tool_code: self.tool_code,
            implementation_code: self.implementation_code,
            input,
            prompt: self.prompt,
            payload,
            attributes,
            actor: ActorDto {
                id: self.actor_id,
                actor_type: self.actor_type,
                display_name: self.actor_name,
            },
            source: SourceDto {
                source_type: self.source_type,
                name: self.source_name,
                metadata: Map::new(),
            },
            correlation: self.correlation_id.map(|correlation_id| CorrelationDto {
                execution_id: None,
                correlation_id: Some(correlation_id),
                causation_id: None,
            }),
            version: None,
            metadata: Map::new(),
        })
    }
}

fn parse_json_object(value: Option<String>) -> Result<Map<String, serde_json::Value>> {
    Ok(match value {
        Some(value) => serde_json::from_str(&value)?,
        None => Map::new(),
    })
}
