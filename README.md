# OOTD 

## Description
People often have trouble finding easily accessible and up-to-date inspiration for their outfits. 
Style-conscious people and fashion enthusiasts need a place to share their outfit ideas, get 
feedback and find inspiration in others. Our app aims to enable users to be able to share their 
outfit of the day with their friends to inspire them and give new style ideas while also suggesting 
brands to buy viewed items from making it easy to replicate outfits that appear in your feed. Users 
are also able to store their previous outfits and highlight ones that they like best.

## Installation

### Clone the Repository
```bash
git clone https://github.com/swent-Team01/OOTD.git
cd OOTD
```

## Project Setup

### System Requirements
- **Java Version**: JDK 17
- **Gradle Version**: 8.13.0
- **Android Version**: Android 15 (API level 35)
- **Target SDK**: 35

## Running the Project

### Local Development
1. **Open the project in Android Studio**
2. **Sync the project** (Using Gradle sync)
3. **Run the app**:
   - Select an emulator or connect a physical device
   - Click the "Run" button

### Command Line Build
```bash
# Build the project
./gradlew build

# Run unit tests
./gradlew test

# Generate code coverage report
./gradlew jacocoTestReport

# Check code formatting
./gradlew ktfmtCheck
```

## Pull Request Guidelines

### Before Submitting a PR

1. **Create a feature branch** from `main` following naming conventions:

   - Feature branches: `feature/#issue_number-short_description`
   - Bug fixes: `fix/#issue_number-short_description`
   - Hotfixes: `hotfix/#issue_number-short_description`

   ```bash
   git checkout -b feature/#issue_number-short_description
   ```
  
2. **Ensure code quality**:
   - Run `./gradlew ktfmtCheck` to verify code formatting
   - Run `./gradlew test` to ensure all unit tests pass
   - Run `./gradlew connectedAndroidTest` for integration tests
   - Maintain or improve code coverage (minimum 80%)

### PR Description Template

```markdown
## Summary
Brief description of what this PR accomplishes

## Changes Made
- List of specific changes
- Use bullet points for clarity

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Related Issues
Closes #issue-number
Unblocks #issue-number (if was blocking any other issue)

## Breaking Changes
List any breaking changes (if any and why)
```

### PR Review Process

1. **Automated Checks**: All PRs must pass CI checks including:
   - Code formatting (KTFmt)
   - Unit and integration tests
   - Code coverage requirements (>= 80%)
   - SonarCloud quality gate

2. **Code Review**: At least one team member must review and approve the PR

3. **Merge Requirements**:
   - All CI checks must pass
   - No merge conflicts
   - Up-to-date with main branch
   - Approved by at least one reviewer

## Ressources :
- [SonarCloud](https://sonarcloud.io/organizations/swent-team01/projects)
- [GitHub](https://github.com/swent-Team01/OOTD)
- [GitHub Wiki](https://github.com/swent-Team01/OOTD/wiki)
- [Figma](https://www.figma.com/design/EQfCuEx3jJpUSZ3NKc4DE5/stefan.taga-s-team-library?t=1rG02nxGubCxY31q-0)

## Our Team
| Name                 | GitHub account                                   |
|----------------------|--------------------------------------------------|
| Kallergis Marc       | [@MarcK0909](https://github.com/MarcK0909)       |
| Steinhauser Corentin | [@cocoStein](https://github.com/cocoStein)       |
| Möbius  Clemens      | [@Clemensito](https://github.com/Clemensito)     |
| Taga    Stefan       | [@stefantaga24](https://github.com/stefantaga24) |
| Meric  Julien        | [@j-meric](https://github.com/j-meric)           |
| Pitu  Bianca         | [@bbianca2004](https://github.com/bbianca2004)   |
| Unluer  Aslì         | [@asunluer](https://github.com/asunluer)         |
