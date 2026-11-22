package {{ cookiecutter.package_name }}.shared.error;

/**
 * Raised when a requested business resource does not exist.
 */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }
}
