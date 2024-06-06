If you want to contribute to the project please create a corresponding issue with the feature/problem you
are working on or comment on an existing issue that you would like to work on it.

To ensure proper formatting you can run
````
gradlew spotlessApply
````
for automatic formatting.

Some remarks on the non-formatting related conventions for this project:
- If possible classes should expose a minimal amout of internal details. It isn't an aim of this project to provide dom introspection.
- If possible classes should be immutable.
- Where possible all fields and method parameters should be marked `@Nullable` or `@NotNull`.
- Accessor methods don't use the `getThing` beans standard but are simply called `thing`.
