# Contributing to OOTD

First off, thank you for considering contributing to OOTD! It's people like you that make OOTD such a great tool for the fashion community.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Workflow](#development-workflow)
- [Style Guidelines](#style-guidelines)
- [Testing Requirements](#testing-requirements)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Pull Request Process](#pull-request-process)

## Code of Conduct

This project and everyone participating in it is governed by our commitment to creating a welcoming and inclusive environment. By participating, you are expected to:

- Use welcoming and inclusive language
- Be respectful of differing viewpoints and experiences
- Gracefully accept constructive criticism
- Focus on what is best for the community
- Show empathy towards other community members

## Getting Started

### Prerequisites

Before you begin, make sure you have:

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Git for version control
- A GitHub account
- Firebase account for testing

### Setting Up Your Development Environment

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR-USERNAME/OOTD.git
   cd OOTD
   ```
3. **Add the upstream repository**:
   ```bash
   git remote add upstream https://github.com/swent-Team01/OOTD.git
   ```
4. **Configure Firebase** (see main README.md for details)
5. **Set up Google Maps API** key in `local.properties`
6. **Build the project** to ensure everything works:
   ```bash
   ./gradlew build
   ```

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the [existing issues](https://github.com/swent-Team01/OOTD/issues) to avoid duplicates.

When creating a bug report, include:

- **Clear title**: Descriptive and specific
- **Steps to reproduce**: Detailed step-by-step instructions
- **Expected behavior**: What should happen
- **Actual behavior**: What actually happens
- **Screenshots/Videos**: Visual evidence if applicable
- **Environment**:
  - Android version
  - Device model
  - App version
- **Additional context**: Any other relevant information

### Suggesting Features

Feature suggestions are welcome! When suggesting a feature:

- **Use a clear title**: Be specific about the feature
- **Provide detailed description**: Explain the feature and its benefits
- **Describe use cases**: When and why would this be useful?
- **Include mockups/examples**: Visual aids help (optional)
- **Consider alternatives**: Are there other ways to achieve this?

### Contributing Code

We welcome code contributions! Here are areas where you can help:

- **Bug fixes**: Check issues labeled `bug`
- **New features**: Check issues labeled `enhancement`
- **Documentation**: Improve existing docs or add new ones
- **Tests**: Increase test coverage
- **Performance**: Optimize existing code
- **Refactoring**: Improve code quality

## Development Workflow

### Branch Strategy

We use the following branch naming conventions:

- `feature/description` - New features
- `fix/description` - Bug fixes
- `refactor/description` - Code refactoring
- `docs/description` - Documentation updates
- `test/description` - Test additions/improvements

### Creating a Feature Branch

```bash
# Update your local main branch
git checkout main
git pull upstream main

# Create and checkout a new branch
git checkout -b feature/your-feature-name
```

### Making Changes

1. **Write your code**: Implement your changes
2. **Follow style guidelines**: Ensure code follows project conventions
3. **Add tests**: Write tests for new functionality
4. **Update documentation**: Update relevant docs if needed
5. **Run tests locally**: Ensure all tests pass

```bash
# Format code
./gradlew ktfmtFormat

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Check code coverage
./gradlew jacocoTestReport
```

## Style Guidelines

### Kotlin Code Style

We use **ktfmt** for code formatting. Run before committing:

```bash
./gradlew ktfmtFormat
```

### General Guidelines

- **Naming**: Use clear, descriptive names for variables, functions, and classes
- **Comments**: Write comments for complex logic, not obvious code
- **Functions**: Keep functions small and focused on a single task
- **Error Handling**: Handle errors gracefully with proper messages
- **Null Safety**: Leverage Kotlin's null safety features
- **Compose**: Follow Jetpack Compose best practices

### File Organization

```
├── model/          # Data models and repositories
├── ui/             # Compose UI components
│   ├── screens/    # Full screen composables
│   ├── components/ # Reusable UI components
│   └── theme/      # Theme and styling
├── utils/          # Utility functions
└── viewmodel/      # ViewModels for business logic
```

## Testing Requirements

### Test Coverage

- **Minimum coverage**: 80% for new code
- **Unit tests**: Required for all business logic
- **Integration tests**: Required for complex features
- **UI tests**: Recommended for critical user flows

### Writing Tests

- **Use descriptive names**: Test names should explain what they test
- **Arrange-Act-Assert**: Follow the AAA pattern
- **Mock dependencies**: Use MockK for Kotlin code
- **Test edge cases**: Don't just test the happy path

Example:
```kotlin
@Test
fun `addItem should update repository and emit success state`() = runTest {
    // Arrange
    val item = createTestItem()
    val repository = mockk<ItemsRepository>()
    coEvery { repository.addItem(item, any()) } just Runs
    
    // Act
    viewModel.addItem(item)
    
    // Assert
    coVerify { repository.addItem(item, any()) }
    assertEquals(UiState.Success, viewModel.uiState.value)
}
```

## Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks
- `perf`: Performance improvements

### Examples

```
feat(wardrobe): add filter by color

Implemented color filtering in wardrobe view with
Material 3 color picker component.

Closes #123
```

```
fix(auth): resolve Google Sign-In crash on Android 13

Updated credential manager to handle Android 13 API changes.
Added proper permission checks for notification access.

Fixes #456
```

## Pull Request Process

### Before Submitting

- [ ] Code follows style guidelines
- [ ] All tests pass locally
- [ ] New tests added for new functionality
- [ ] Documentation updated if needed
- [ ] No merge conflicts with main branch
- [ ] Code coverage maintained or improved

### Submitting a Pull Request

1. **Push your changes** to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Create a Pull Request** on GitHub:
   - Use a clear, descriptive title
   - Fill out the PR template completely
   - Link related issues (e.g., "Closes #123")
   - Add screenshots/videos for UI changes
   - Request review from maintainers

3. **Address feedback**:
   - Respond to review comments
   - Make requested changes
   - Push updates to the same branch
   - Re-request review when ready

4. **After approval**:
   - Maintainer will merge your PR
   - Delete your feature branch after merge
   - Update your local main branch:
     ```bash
     git checkout main
     git pull upstream main
     ```

### PR Review Criteria

Reviewers will check:

- **Functionality**: Does it work as intended?
- **Code Quality**: Is the code clean and maintainable?
- **Tests**: Are there sufficient tests?
- **Documentation**: Is documentation updated?
- **Performance**: Does it impact app performance?
- **Security**: Are there any security concerns?

## Getting Help

If you need help at any point:

- **Documentation**: Check the [GitHub Wiki](https://github.com/swent-Team01/OOTD/wiki)
- **Discussions**: Post in [GitHub Discussions](https://github.com/swent-Team01/OOTD/discussions)
- **Issues**: Comment on relevant issues
- **Ask the team**: Reach out to maintainers

## Recognition

Contributors will be recognized in:

- Pull request acknowledgments
- Release notes
- Project documentation

Thank you for contributing to OOTD! 🎉

