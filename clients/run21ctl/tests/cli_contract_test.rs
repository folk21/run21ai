use clap::Parser;
use run21ctl::cli::{Cli, Command};

#[test]
fn parses_health_command_with_default_base_url() {
    let cli = Cli::parse_from(["run21ctl", "health"]);

    assert_eq!("http://127.0.0.1:8080", cli.base_url);
    assert!(matches!(cli.command, Command::Health));
}

#[test]
fn parses_tool_descriptor_command() {
    let cli = Cli::parse_from(["run21ctl", "tool", "formula.evaluate"]);

    match cli.command {
        Command::Tool { code } => assert_eq!("formula.evaluate", code),
        other => panic!("unexpected command: {other:?}"),
    }
}

#[test]
fn builds_structured_execution_request_from_flags() {
    let cli = Cli::parse_from([
        "run21ctl",
        "execute",
        "--tool-code",
        "formula.evaluate",
        "--implementation-code",
        "formula-evalex-v1",
        "--input-json",
        r#"{"expression":"a + b","variables":{"a":10,"b":11}}"#,
        "--actor-id",
        "manual-user",
        "--actor-name",
        "Manual User",
        "--correlation-id",
        "contract-test",
    ]);

    let request = match cli.command {
        Command::Execute(args) => args.into_request().expect("request should build"),
        other => panic!("unexpected command: {other:?}"),
    };

    assert_eq!("structured", request.request_type);
    assert_eq!(Some("formula.evaluate".to_string()), request.tool_code);
    assert_eq!(
        Some("formula-evalex-v1".to_string()),
        request.implementation_code
    );
    assert_eq!("manual-user", request.actor.id);
    assert_eq!(
        Some("contract-test".to_string()),
        request.correlation.unwrap().correlation_id
    );
    assert_eq!("a + b", request.input["expression"]);
}

#[test]
fn builds_prompt_request_from_prompt_flag() {
    let cli = Cli::parse_from([
        "run21ctl",
        "resolve",
        "--prompt",
        "calculate 10 + 11",
        "--actor-id",
        "manual-user",
    ]);

    let request = match cli.command {
        Command::Resolve(args) => args.into_request().expect("request should build"),
        other => panic!("unexpected command: {other:?}"),
    };

    assert_eq!("prompt", request.request_type);
    assert_eq!(Some("calculate 10 + 11".to_string()), request.prompt);
    assert_eq!("manual-user", request.actor.id);
}

#[test]
fn rejects_invalid_json_payload() {
    let cli = Cli::parse_from([
        "run21ctl",
        "execute",
        "--tool-code",
        "formula.evaluate",
        "--input-json",
        "not-json",
    ]);

    let result = match cli.command {
        Command::Execute(args) => args.into_request(),
        other => panic!("unexpected command: {other:?}"),
    };

    assert!(result.is_err());
}
