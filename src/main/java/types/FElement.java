package types;

public class FElement {
    private final int NWORDS_FIELD = 4; // TODO True for x86, ARM

    typedef digit_t felm_t[NWORDS_FIELD];

    private int[] content = new int[NWORDS_FIELD];
}
