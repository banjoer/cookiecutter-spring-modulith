package {{ cookiecutter.package_name }}.shared.error;

/**
 * Raised when a request conflicts with existing state, for example a duplicate username.
 */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
