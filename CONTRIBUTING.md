# How to contribute to nakadi-producer-spring-boot-starter

Thanks for contributing to our project! We really appreciate that.

We are looking for all kinds of contributions, for example those:

* Try out the library in your project, and tell us about your experiences.
* If you see anything which could be improved, please open an issue (or comment on an existing one where it fits).
* If you find something which is wrong, please open an issue, too.
* If you want to implement an improvement or bugfix yourself, even better – just open a pull request.
* You can also review code contributions from other people (or even from the maintainers), and/or comment on bug reports or feature requests.

We are also happy about contributions to the documentation (via pull request), as well as helping to spread knowledge about our project (social media, blog posts, ...).

## Feedback

### How to report a bug

**If you found a security problem which should preferably not discussed publicly, please send an email to [the maintainers](MAINTAINERS), with a copy to tech-security@zalando.de, instead of opening an issue.**

Before opening a new issue for a bug report, please have a look whether there already exists a similar issue. If so, please just comment there, adding your observations.

When reporting a bug, it is very helpful if you can provide enough details to reproduce it. Ideal is a complete minimal example which shows the problem. Please also mention what behavior you expect, and what you actually observe. Always include the version of the library you are using.

If a bug is already fixed with the latest released version, we will likely decide not to create bugfix releases for older major/minor versions, unless those are critical security bugs.

### How to suggest a feature or enhancement

We welcome all kinds of suggestions for new features or other improvements of our library. Please open an issue in the Github issue tracker. Please be as specific as you can, but if you can't, also more general suggestions are welcome.

In comments to the issue we can discuss whether this is something we actually want, whether it may already exist (just not documented enough), and how it should look like in detail.

Feel free to also participate in discussions to other people's enhancement requests.

### Other kinds of feedback

If you just want to report your experiences, just open an issue (please include "experience report" in the title). Compared to emails to the maintainers, this is more open and visible to the public. (Although, if we find nothing actionable in there, we'll label it and then close it with a "Thank you".)

## Contributing code or documentation

For bug fixes and small improvements where you already know how to implement them, you can just open a pull request with the changes.

For features where you would have to invest more work, or where you need help in finding the right way of doing it, please open an issue first so we can discuss whether it goes into the direction we want to go, and so the maintainers can give you hints on where to start. (Just mention in the issue that you are willing to work on this, and whether you need some support.)

If you want to contribute, but don't know what, all [issues labeled with "help wanted"](https://github.com/zalando-nakadi/nakadi-producer-spring-boot-starter/labels/help%20wanted) are good to start with.

### Pull requests

New contributors, please fork the project, then create a branch in your fork with a suitable name, add one or more commits with your change, and open the pull request from there. Regular contributors can get write access to the main repository to be able to create feature branches there. (This makes it easier for multiple people to collaborate on one branch.)

Even if you are a maintainer: never commit directly to master, always use pull-requests, so others can review your changes.

Please add some text in the initial comment of the pull request describing why you are doing the change. If you have just one commit, Github will automatically fill this from the commit message, see next section. For multi-commit PRs, you'll need too summarize this manually.

If you are implementing an issue, please mention its number in the PR comment, so reviewers (and people digging through the history later) can look up what this is about. (This will also create a link in the other direction, which is useful for people finding the issue first.)

### Commit messages

Please try to explain why you did your changes in the commit messages. If you are implementing an issue, include its number in the message.

### Tests

If you are fixing a bug, please try to also add a test which would fail without the fix, thereby making sure it doesn't happen again. If you are adding a new feature, please also add tests for it.

The changed code from your pull request will be automatically run on our continuous integration system (Travis CI), it will send its feedback back, and a non-failing build + tests is a requirement for merging. (You can click on the link to see what did go wrong.)

### Code review

For a pull request to be merged, it needs at least two comments with just a ":+1:" in them (you can type `:+1:`) from members of the Zalando Github organization (i.e. software engineers at Zalando), then a maintainer can merge it. We also appreciate other interested persons to review our and other's code (but for compliance reasons, their :+1: don't count).

Just as for issues, the maintainers try to respond to every pull request in 72 hours (excluding weekends).

## Conduct

In the interest of fostering an open and welcoming environment, we follow and enforce our [Code of Conduct](https://github.com/zalando-nakadi/nakadi-producer-spring-boot-starter/blob/master/CODE_OF_CONDUCT.md).
