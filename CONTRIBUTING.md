# Contributing to AI Course Generation Platform

First off, thank you for considering contributing to AI Course Generation Platform! 🎉

We welcome contributions of all kinds, including:

- Bug fixes
- New features
- Performance improvements
- Documentation updates
- UI/UX enhancements
- Testing improvements

---

## Before You Start

Before creating a new issue or pull request:

1. Search existing issues to avoid duplicates.
2. Check if the feature or bug has already been discussed.
3. Open an issue first for large changes so we can discuss the implementation approach.

---

## Looking for Something to Work On?

Check issues labeled:

* good first issue
* help wanted
* enhancement

These are great starting points for new contributors.

---

## Development Setup

### Prerequisites

- Docker
- Docker Compose
- Git

### Clone Repository

```bash
git clone https://github.com/Aazhii/CourseGen

cd CourseGen
````

### Run the Application

```bash
docker-compose up --build -d
```

### View Logs

```bash
docker-compose logs -f
```

### Stop Services

```bash
docker-compose down
```

---

## Accessing the Services

### Frontend & Backend

http://localhost:3000 , http://localhost:8080


### PostgreSQL

Host: localhost:5432 , DB : aicourse

Username/Password : postgres/password

---

## Branch Naming Convention

Please use the following naming format:

```text
feature/add-course-export
feature/ai-quiz-generation

bugfix/fix-authentication
bugfix/course-loading-issue

docs/update-readme

refactor/course-service-cleanup
```
---

## Pull Request Process

1. Fork the repository.
2. Create a new branch.
3. Implement your changes.
4. Test thoroughly.
5. Commit and push your changes.
6. Open a Pull Request.

---

## Pull Request Checklist

Before submitting a pull request, ensure:

* [ ] Project builds successfully
* [ ] Existing functionality remains unaffected
* [ ] Code follows project conventions
* [ ] Documentation is updated where necessary
* [ ] New functionality is tested
      
---

## Questions?

If you have questions about the project, feel free to open a GitHub Discussion or create an issue.
