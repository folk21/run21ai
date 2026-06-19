# Architecture Documentation Map

This folder describes the system architecture.

## Reading Order

1. [Architecture Overview](architecture-overview.md)
2. [Documentation Profile](documentation-profile.md)
3. [arc42 Mapping](arc42-mapping.md)
4. [Runtime View](runtime-view.md)
5. [Deployment View](deployment-view.md)
6. [Quality Attributes](quality-attributes.md)
7. [Risks and Technical Debt](risks-and-technical-debt.md)
8. [C4 Diagrams](c4/README.md)
9. [ADRs](adr/README.md)

## Architecture Style

Run21AI follows pragmatic clean architecture:

- the core defines abstractions;
- runtime modules provide concrete behavior;
- dependencies point inward;
- framework-specific code stays outside the core.
