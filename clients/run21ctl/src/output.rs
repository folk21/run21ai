use anyhow::Result;
use serde::Serialize;

/// Prints a response object as stable, pretty JSON.
pub fn print_json<T: Serialize>(value: &T) -> Result<()> {
    println!("{}", serde_json::to_string_pretty(value)?);
    Ok(())
}
