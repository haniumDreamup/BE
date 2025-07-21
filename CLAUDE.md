# BIF-AI Reminder Backend

Cognitive assistance system for users with IQ 70-85. Spring Boot backend with AI-powered situational awareness.

## Tech Stack
- Spring Boot 3.5.0, Java 17
- MySQL 8.0, Redis
- AWS (EC2, RDS, S3), OpenAI API
- Gradle, Docker
- Spring Security with JWT

## Commands

### Task Management
```bash
npx task-master list                              # View all tasks
npx task-master show <id>                         # Task details with subtasks
npx task-master set-status --id=<id> --status=<status>  # Update task/subtask status
npx task-master expand --id=<id>                  # Show task breakdown
```

### Development
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'  # Run with profile
./gradlew test                                    # All tests
./gradlew test --tests *ServiceTest               # Service layer tests
./gradlew build -x test                          # Build without tests
./gradlew clean build                            # Clean build
```

### Database
```bash
./gradlew flywayMigrate                          # Run migrations
./gradlew flywayClean                            # Clean database
mysql -u bifai_user -p bifai_db                  # Connect to DB
```

## Code Style
- 2-space indentation for Java
- camelCase for variables, PascalCase for classes
- Max line length: 120 characters
- Always use @Slf4j for logging
- Prefer constructor injection over @Autowired
- All DTOs must use @Valid annotations
- Entity fields must have @Column annotations

## Do Not
- Use System.out.println() - use log instead
- Return null - use Optional
- Catch generic Exception - be specific
- Use magic numbers - create constants
- Skip input validation
- Expose internal exceptions to API responses
- Use complex language in user-facing messages
- Create endpoints without authentication



## BIF Requirements
- All text responses: 5th-grade reading level
- Error messages: Positive, simple language
- Instructions: Step-by-step with visual cues
- UI elements: Minimum 48dp touch targets
- Navigation: Maximum 2-level depth
- Response time: < 3 seconds for all operations
- Fallback: Offline mode for critical features

## API Conventions

### Response Format
```json
{
  "success": true,
  "data": {},
  "message": "Operation completed successfully",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "USER_FRIENDLY_CODE",
    "message": "Simple explanation of what happened",
    "userAction": "What the user can do to fix it"
  },
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### HTTP Status Codes
- 200: Success
- 201: Created
- 400: User error (with friendly message)
- 401: Not authenticated
- 403: Not authorized
- 404: Not found
- 500: Server error (generic message to user)

## Testing Standards
- Unit test coverage: Minimum 80%
- Integration tests: All API endpoints
- Performance: Support 100+ concurrent users
- Each test class must use @SpringBootTest or @DataJpaTest
- Mock external services with @MockBean
- Test data: Use @Sql for database state

## Environment Variables
```bash
DB_HOST=localhost
DB_PORT=3306
DB_NAME=bifai_db
DB_USER=bifai_user
DB_PASSWORD=<secure>

REDIS_HOST=localhost
REDIS_PORT=6379

AWS_REGION=ap-northeast-2
S3_BUCKET_NAME=bifai-images

OPENAI_API_KEY=<key>
JWT_SECRET=<secret>
JWT_EXPIRATION=86400000
```

## Critical Context
- Emergency features: Highest priority
- Guardian access: Must verify relationship
- Data privacy: Encrypt PII, minimize collection
- Accessibility: WCAG 2.1 AA compliance
- Performance: <500ms API response, <3s AI analysis

## Korean Language Context
This is a korean-focused projects
- All UI text is in korean
- using korean

## implementation consideration
- write researched best practice in text or md file to use as reference
- reference /Users/ihojun/Desktop/javaWorkSpace/BE/.cursor/rules/taskmaster/dev_workflow.mdc
- keep SOLID principle
