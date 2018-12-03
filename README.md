# Default Dependencies Problem - Minimal Example

Running `./gradlew build` from the composite build produces this error:

    Could not determine the dependencies of task ':plugin-user:plugin-user.feature:assemble'.
    > Cannot change dependencies of configuration ':plugin-user:plugin-user.feature:myPlugin'
      after it has been included in dependency resolution.

However, each of the sub builds run fine on its own, and the problem does not
appear when not adding one of the default dependencies (in `MyPluginRepository`
or `MyPluginFeature`).




