use anyhow::Result;
use clap::Parser;
use run21ctl::cli::{Cli, Command};
use run21ctl::client::Run21Client;
use run21ctl::config::Config;
use run21ctl::output::print_json;

#[tokio::main]
async fn main() -> Result<()> {
    let cli = Cli::parse();
    let config = Config::from_cli(&cli);
    let client = Run21Client::new(config)?;

    match cli.command {
        Command::Health => print_json(&client.health().await?)?,
        Command::Ready => print_json(&client.ready().await?)?,
        Command::Tools => print_json(&client.tools().await?)?,
        Command::Tool { code } => print_json(&client.tool(&code).await?)?,
        Command::Execute(args) => {
            let request = args.into_request()?;
            print_json(&client.execute(&request).await?)?;
        }
        Command::Resolve(args) => {
            let request = args.into_request()?;
            print_json(&client.resolve(&request).await?)?;
        }
    }

    Ok(())
}
