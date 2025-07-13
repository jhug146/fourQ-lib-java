package types.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * A robust generic pair data structure that holds two related values.
 * 
 * This implementation provides a feature-complete pair class similar to Kotlin's Pair,
 * with proper equals/hashCode implementation, serialization support, and utility methods.
 * 
 * @param <T> the type of the first element
 * @param <S> the type of the second element
 * 
 * @author Naman Malhotra, James Hughff
 * @since 1.0
 */
public class Pair<T, S> implements Serializable, Cloneable {
    @Serial
    private static final long serialVersionUID = 1L;

    public T first;
    public S second;
    
    /**
     * Creates a new pair with the specified elements.
     * 
     * @param first the first element
     * @param second the second element
     */
    public Pair(@Nullable T first, @Nullable S second) {
        this.first = first;
        this.second = second;
    }
    
    /**
     * Static factory method to create a new pair.
     * This provides a more fluent API similar to Kotlin's Pair constructor.
     * 
     * @param <T> the type of the first element
     * @param <S> the type of the second element
     * @param first the first element
     * @param second the second element
     * @return a new Pair instance
     */
    @NotNull
    public static <T, S> Pair<T, S> of(@Nullable T first, @Nullable S second) {
        return new Pair<>(first, second);
    }

    @Nullable
    public T getFirst() {
        return first;
    }

    @Nullable
    public S getSecond() {
        return second;
    }

    public void setFirst(@Nullable T first) {
        this.first = first;
    }

    public void setSecond(@Nullable S second) {
        this.second = second;
    }
    
    /**
     * Returns a new pair with the first element replaced.
     * 
     * @param <U> the type of the new first element
     * @param newFirst the new first element
     * @return a new Pair with the updated first element
     */
    @NotNull
    public <U> Pair<U, S> withFirst(@Nullable U newFirst) {
        return new Pair<>(newFirst, this.second);
    }
    
    /**
     * Returns a new pair with the second element replaced.
     * 
     * @param <U> the type of the new second element
     * @param newSecond the new second element
     * @return a new Pair with the updated second element
     */
    @NotNull
    public <U> Pair<T, U> withSecond(@Nullable U newSecond) {
        return new Pair<>(this.first, newSecond);
    }

    @NotNull
    public Pair<S, T> swap() {
        return new Pair<>(this.second, this.first);
    }
    
    /**
     * Converts this pair to an array containing both elements.
     * Note: This method returns Object[] due to type erasure limitations.
     * 
     * @return an array containing [first, second]
     */
    @NotNull
    public Object[] toArray() {
        return new Object[]{first, second};
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Pair<?, ?> pair = (Pair<?, ?>) obj;
        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
    
    @Override
    @NotNull
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
    
    @Override
    @NotNull
    public Pair<T, S> clone() {
        try {
            @SuppressWarnings("unchecked")
            Pair<T, S> cloned = (Pair<T, S>) super.clone();
            return cloned;
        } catch (CloneNotSupportedException e) {
            // This should never happen since we implement Cloneable
            throw new AssertionError("Clone not supported", e);
        }
    }
    
    /**
     * Performs a deep clone if the elements implement Cloneable, otherwise performs a shallow clone.
     * This method attempts to clone the individual elements if they support cloning.
     * 
     * @return a deep clone of this pair if possible, otherwise a shallow clone
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public Pair<T, S> deepClone() {
        T clonedFirst = first;
        S clonedSecond = second;
        
        // Attempt to clone first element if it's Cloneable
        if (first instanceof Cloneable) {
            try {
                clonedFirst = (T) first.getClass().getMethod("clone").invoke(first);
            } catch (Exception e) {
                // If cloning fails, use the original reference
                clonedFirst = first;
            }
        }
        
        // Attempt to clone second element if it's Cloneable
        if (second instanceof Cloneable) {
            try {
                clonedSecond = (S) second.getClass().getMethod("clone").invoke(second);
            } catch (Exception e) {
                // If cloning fails, use the original reference
                clonedSecond = second;
            }
        }
        
        return new Pair<>(clonedFirst, clonedSecond);
    }
    
    /**
     * Checks if both elements of the pair are non-null.
     * 
     * @return true if both first and second are non-null
     */
    public boolean isComplete() {
        return first != null && second != null;
    }
    
    /**
     * Checks if either element of the pair is null.
     * 
     * @return true if either first or second is null
     */
    public boolean hasNull() {
        return first == null || second == null;
    }
}
