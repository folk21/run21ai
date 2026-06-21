use anyhow::Result;
use reqwest::StatusCode;
use serde::de::DeserializeOwned;
use serde_json::Value;

use crate::config::{endpoint, Config};
use crate::dto::{ExecutionRequestDto, ToolDescriptorDto};

/// Small HTTP client for the Run21AI REST API.
pub struct Run21Client {
    config: Config,
    http: reqwest::Client,
}

impl Run21Client {
    /// Creates a client with the provided runtime configuration.
    pub fn new(config: Config) -> Result<Self> {
        Ok(Self {
            config,
            http: reqwest::Client::new(),
        })
    }

    pub async fn health(&self) -> Result<Value> {
        self.get_json("/health").await
    }

    pub async fn ready(&self) -> Result<Value> {
        self.get_json("/ready").await
    }

    pub async fn tools(&self) -> Result<Vec<ToolDescriptorDto>> {
        self.get_json("/v1/tools").await
    }

    pub async fn tool(&self, code: &str) -> Result<Value> {
        self.get_json(&format!("/v1/tools/{code}")).await
    }

    pub async fn execute(&self, request: &ExecutionRequestDto) -> Result<Value> {
        self.post_json("/v1/executions", request).await
    }

    pub async fn resolve(&self, request: &ExecutionRequestDto) -> Result<Value> {
        self.post_json("/v1/tool-requests/resolve", request).await
    }

    async fn get_json<T: DeserializeOwned>(&self, path: &str) -> Result<T> {
        let url = endpoint(&self.config.base_url, path)?;
        let response = self.http.get(url).send().await?;
        read_json(response).await
    }

    async fn post_json<T: DeserializeOwned>(
        &self,
        path: &str,
        request: &ExecutionRequestDto,
    ) -> Result<T> {
        let url = endpoint(&self.config.base_url, path)?;
        let response = self.http.post(url).json(request).send().await?;
        read_json(response).await
    }
}

async fn read_json<T: DeserializeOwned>(response: reqwest::Response) -> Result<T> {
    let status = response.status();
    if status != StatusCode::OK {
        let text = response.text().await.unwrap_or_default();
        anyhow::bail!("REST API returned {status}: {text}");
    }
    Ok(response.json().await?)
}
