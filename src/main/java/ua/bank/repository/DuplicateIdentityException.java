package ua.bank.repository;

public class DuplicateIdentityException extends IllegalStateException {

    private final String repositoryName;
    private final String identity;

    public DuplicateIdentityException(String repositoryName, String identity) {
        super("Duplicate identity '" + identity + "' in repository '" + repositoryName + "'");
        this.repositoryName = repositoryName;
        this.identity = identity;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getIdentity() {
        return identity;
    }
}