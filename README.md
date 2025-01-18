# Search Engine Project

A modern search engine built with Spring Boot and gRPC capabilities.

## Project Setup

### Prerequisites
- JDK 17 or later 
- Maven 3.6+
- Docker
- IntelliJ IDEA

### Spring Configuration in IntelliJ IDEA

1. **Project Configuration**
   - Open IntelliJ IDEA
   - Go to `File -> Project Structure`
   - Ensure SDK is set to JDK 17+
   - Set language level to "17 - Sealed types, always-strict floating-point semantics"

2. **Spring Boot Configuration**
   - Enable annotation processing: `Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors`
   - Check "Enable annotation processing"
   - Set the processor path to "Module content root"

3. **Run Configurations in IntelliJ**

   #### leader node configuration
   - Click "Edit Configurations..."
   - Create new Spring Boot configuration
   - Name: `search-engine-leader`
   - Main class: `ds.searchengine.SearchEngineApplication`
   - Program arguments: ` --grpc.server.port=9090 --spring.profiles.active=leader`
   - Environment variables: 
     - `ROLE=leader`
     - `LEADER_PORT=9090`

   #### worker node configuration
   - Create another Spring Boot configuration
   - Name: `search-engine-worker`
   - Main class: `ds.searchengine.SearchEngineApplication`
   - Program arguments: ` --grpc.server.port=9091 --spring.profiles.active=worker`
   - Environment variables:
     - `ROLE=worker`
     - `LEADER_HOST=localhost`
     - `LEADER_PORT=9090`

   Multiple worker configurations can be created by incrementing the ports:
   - worker 1:  grpc.server.port=9091
   - worker 2: grpc.server.port=9092

### Running the Cluster
1. Start the leader node first using the `search-engine-leader` configuration
2. Start one or more worker nodes using the `search-engine-worker` configurations

### Port Configuration Reference
- gRPC Ports:
  - leader: 9090
  - workers: 9091, 9092, ...

## Development

### Local Development
```bash
./mvnw spring-boot:run
