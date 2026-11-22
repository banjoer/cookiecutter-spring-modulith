/**
 * Shared kernel for a very small set of cross-module value types.
 *
 * <p>This package is registered as a shared module in {@code @Modulithic}. It must not depend on
 * any business module, and it must not become a home for repositories, entities, or utilities that
 * belong to a single module.</p>
 */
package {{ cookiecutter.package_name }}.shared;
