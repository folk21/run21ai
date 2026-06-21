use anyhow::{anyhow, Result};
use reqwest::Url;

use crate::cli::Cli;

/// Runtime configuration for the CLI process.
#[derive(Debug, Clone)]
pub struct Config {
    pub base_url: Url,
}

impl Config {
    /// Builds configuration from parsed CLI flags and environment-derived values.
    pub fn from_cli(cli: &Cli) -> Self {
        Self {
            base_url: normalize_base_url(&cli.base_url),
        }
    }
}

fn normalize_base_url(value: &str) -> Url {
    let trimmed = value.trim_end_matches('/');
    Url::parse(trimmed).unwrap_or_else(|error| {
        panic!("Invalid Run21AI REST API base URL '{value}': {error}");
    })
}

/// Joins a REST path with the configured base URL.
pub fn endpoint(base_url: &Url, path: &str) -> Result<Url> {
    let path = path.trim_start_matches('/');
    base_url
        .join(path)
        .map_err(|error| anyhow!("Failed to build endpoint URL for '{path}': {error}"))
}
