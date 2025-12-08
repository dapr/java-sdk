# Contribution Guidelines

Thank you for your interest in Dapr!

This project welcomes contributions and suggestions. Most contributions require you to signoff on your commits via 
the Developer Certificate of Origin (DCO). When you submit a pull request, a DCO-bot will automatically determine 
whether you need to provide signoff for your commit. Please follow the instructions provided by DCO-bot, as pull 
requests cannot be merged until the author(s) have provided signoff to fulfill the DCO requirement.
You may find more information on the DCO requirements [below](#developer-certificate-of-origin-signing-your-work).

This project has adopted the [Contributor Covenant Code of Conduct](https://github.com/dapr/community/blob/master/CODE-OF-CONDUCT.md).

Contributions come in many forms: submitting issues, writing code, participating in discussions and community calls.

To learn more about becoming a contributor and the different roles within the Dapr community (Contributor, Approver, Maintainer), please refer to our [Community Membership](https://github.com/dapr/community/blob/master/community-membership.md) documentation.

This document provides the guidelines for how to contribute to the Dapr project.

## Issues

This section describes the guidelines for submitting issues

### Issue Types

There are 4 types of issues:

- Issue/Bug: You've found a bug with the code, and want to report it, or create an issue to track the bug.
- Issue/Discussion: You have something on your mind, which requires input form others in a discussion, before it eventually manifests as a proposal.
- Issue/Proposal: Used for items that propose a new idea or functionality. This allows feedback from others before code is written.
- Issue/Question: Use this issue type, if you need help or have a question.

### Before You File

Before you file an issue, make sure you've checked the following:

1. Is it the right repository?
    - The Dapr project is distributed across multiple repositories. Check the list of [repositories](https://github.com/dapr) if you aren't sure which repo is the correct one.
1. Check for existing issues
    - Before you create a new issue, please do a search in [open issues](https://github.com/dapr/java-sdk/issues) to see if the issue or feature request has already been filed.
    - If you find your issue already exists, make relevant comments and add your [reaction](https://github.com/blog/2119-add-reaction-to-pull-requests-issues-and-comments). Use a reaction:
        - 👍 up-vote
        - 👎 down-vote
1. For bugs
    - Check it's not an environment issue. For example, if running on Kubernetes, make sure prerequisites are in place. (state stores, bindings, etc.)
    - You have as much data as possible. This usually comes in the form of logs and/or stacktrace. If running on Kubernetes or other environment, look at the logs of the Dapr services (runtime, operator, placement service). More details on how to get logs can be found [here](https://docs.dapr.io/operations/troubleshooting/logs-troubleshooting/).
1. For proposals
    - Many changes to the Dapr runtime may require changes to the API. In that case, the best place to discuss the potential feature is the main [Dapr repo](https://github.com/dapr/dapr).
    - Other examples could include bindings, state stores or entirely new components.

## Contributing to Dapr

This section describes the guidelines for contributing code / docs to Dapr.

### Things to consider when adding new API to SDK

1. All the new API's go under [dapr-sdk maven package](https://github.com/dapr/java-sdk/tree/master/sdk)
2. Make sure there is an example talking about how to use the API along with a README with mechanical markdown. [Example](https://github.com/dapr/java-sdk/pull/1235/files#diff-69ed756c4c01fd5fa884aac030dccb8f3f4d4fefa0dc330862d55a6f87b34a14)

#### Mechanical Markdown

Mechanical markdown is used to validate example outputs in our CI pipeline. It ensures that the expected output in README files matches the actual output when running the examples. This helps maintain example output, catches any unintended changes in example behavior, and regressions.

To test mechanical markdown locally:

1. Install the package:
```bash
pip3 install mechanical-markdown
```

2. Run the test from the respective examples README directory, for example:
```bash
cd examples
mm.py ./src/main/java/io/dapr/examples/workflows/README.md
```

The test will:
- Parse the STEP markers in the README
- Execute the commands specified in the markers
- Compare the actual output with the expected output
- Report any mismatches

When writing STEP markers:
- Use `output_match_mode: substring` for flexible matching
- Quote strings containing special YAML characters (like `:`, `*`, `'`)
- Set appropriate timeouts for long-running examples

Example STEP marker:
```yaml
<!-- STEP
name: Run example
output_match_mode: substring
expected_stdout_lines:
  - "Starting workflow: io.dapr.examples.workflows.compensation.BookTripWorkflow"
  ...
background: true
timeout_seconds: 60
-->
```

### Pull Requests

All contributions come through pull requests. To submit a proposed change, we recommend following this workflow:

1. Make sure there's an issue (bug or proposal) raised, which sets the expectations for the contribution you are about to make.
2. Fork the relevant repo and create a new branch
3. Create your change
    - Code changes require tests
4. Update relevant documentation for the change
5. Check the code style
6. Commit and open a PR
7. Wait for the CI process to finish and make sure all checks are green
8. A maintainer of the project will be assigned, and you can expect a review within a few days
9. All the files have the Copyright header.

### Configure the code style with checkstyle

The project contains a checkstyle file (`checkstyle.xml`) that must be used for formatting the code.

* IntelliJ IDEA
   You can use [CheckStyle-IDEA plugin](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea).
* VSCode
   You can use [Checkstyle for Java](https://marketplace.visualstudio.com/items?itemName=shengchen.vscode-checkstyle).
* Eclipse
  You can use [Eclipse Checkstyle Plugin](https://checkstyle.org/eclipse-cs/#!/)

#### Use work-in-progress PRs for early feedback

A good way to communicate before investing too much time is to create a "Work-in-progress" PR and share it with your reviewers. The standard way of doing this is to add a "[WIP]" prefix in your PR's title and assign the **do-not-merge** label. This will let people looking at your PR know that it is not well baked yet.

### Developer Certificate of Origin: Signing your work

#### Every commit needs to be signed

The Developer Certificate of Origin (DCO) is a lightweight way for contributors to certify that they wrote or otherwise have the right to submit the code they are contributing to the project. Here is the full text of the [DCO](https://developercertificate.org/), reformatted for readability:
```
By making a contribution to this project, I certify that:

    (a) The contribution was created in whole or in part by me and I have the right to submit it under the open source license indicated in the file; or

    (b) The contribution is based upon previous work that, to the best of my knowledge, is covered under an appropriate open source license and I have the right under that license to submit that work with modifications, whether created in whole or in part by me, under the same open source license (unless I am permitted to submit under a different license), as indicated in the file; or

    (c) The contribution was provided directly to me by some other person who certified (a), (b) or (c) and I have not modified it.

    (d) I understand and agree that this project and the contribution are public and that a record of the contribution (including all personal information I submit with it, including my sign-off) is maintained indefinitely and may be redistributed consistent with this project or the open source license(s) involved.
```

Contributors sign-off that they adhere to these requirements by adding a `Signed-off-by` line to commit messages.

```
This is my commit message

Signed-off-by: Random J Developer <random@developer.example.org>
```
Git even has a `-s` command line option to append this automatically to your commit message:
```
$ git commit -s -m 'This is my commit message'
```

Each Pull Request is checked  whether or not commits in a Pull Request do contain a valid Signed-off-by line.

#### I didn't sign my commit, now what?!

No worries - You can easily replay your changes, sign them and force push them!

```
git checkout <branch-name>
git commit --amend --no-edit --signoff
git push --force-with-lease <remote-name> <branch-name>
```

### Use of Third-party code

- All third-party code must be placed in the `vendor/` folder.
- `vendor/` folder is managed by Go modules and stores the source code of third-party Go dependencies. - The `vendor/` folder should not be modified manually.
- Third-party code must include licenses.

A non-exclusive list of code that must be places in `vendor/`:

- Open source, free software, or commercially-licensed code.
- Tools or libraries or protocols that are open source, free software, or commercially licensed.

**Thank You!** - Your contributions to open source, large or small, make projects like this possible. Thank you for taking the time to contribute.

## Github Dapr Bot Commands

Checkout the [daprbot documentation](https://docs.dapr.io/contributing/daprbot/) for Github commands you can run in this repo for common tasks. For example, you can run the `/assign` (as a comment on an issue) to assign the issue to yourself.

## Code of Conduct

This project has adopted the [Contributor Covenant Code of Conduct](https://github.com/dapr/community/blob/master/CODE-OF-CONDUCT.md)
