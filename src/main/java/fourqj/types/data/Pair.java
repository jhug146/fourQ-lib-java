package fourqj.types.data;

import java.io.Serializable;
import java.util.Objects;

import static fourqj.utils.StringUtils.buildString;


/**
 * A robust generic pair data structure that holds two related values.
 * <p>
 * This implementation provides a feature-complete pair class similar to Kotlin's Pair,
 * with proper equals/hashCode implementation, serialization support, and utility methods.
 *
 * @param <T> the type of the first element
 * @param <S> the type of the second element
 *
 * @author Naman Malhotra, James Hughff
 * @since 1.0.0
 */
public class Pair<T, S> implements Serializable, Cloneable {
    public T first;
    public S second;
    
    /**
     * Creates a new pair with the specified elements.
     * 
     * @param first the first element
     * @param second the second element
     */
    public Pair(T first, S second) {
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
    public static <T, S> Pair<T, S> of(T first, S second) {
        return new Pair<>(first, second);
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
    public String toString() {
        return buildString(sb -> {
            sb.append("(");
            sb.append(first);
            sb.append(", ");
            sb.append(second);
            sb.append(")");
        });
    }
}
