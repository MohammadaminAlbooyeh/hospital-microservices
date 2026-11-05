# Generated contract artifacts

`contracts/codegen.sh` is best-effort and skips tools not installed locally/CI.

- Java protobuf/gRPC/Avro outputs target `services-java/shared-kernel/src/main/java` when tools are available.
- Python Pydantic models target `services-python/shared/hospital_shared/events/generated.py`.
- If generation is skipped, services should continue using their checked-in/manual models until toolchain setup is standardized.
