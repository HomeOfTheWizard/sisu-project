/*******************************************************************************
 * Copyright (c) 2010, 2013 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stuart McCulloch (Sonatype, Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.inject;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * NON-thread-safe {@link Map} whose keys are kept alive by soft/weak {@link Reference}s.
 */
class MildKeys<K, V>
    implements Map<K, V>
{
    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    private final ReferenceQueue<K> queue = new ReferenceQueue<K>();

    private final Map<Reference<K>, V> map;

    private final boolean soft;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    MildKeys( final Map<Reference<K>, V> map, final boolean soft )
    {
        this.map = map;
        this.soft = soft;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public final boolean containsKey( final Object key )
    {
        // skip compact for performance reasons

        return map.containsKey( tempKey( key ) );
    }

    public final boolean containsValue( final Object value )
    {
        // skip compact for performance reasons

        return map.containsValue( value );
    }

    public final V get( final Object key )
    {
        // skip compact for performance reasons

        return map.get( tempKey( key ) );
    }

    public final V put( final K key, final V value )
    {
        compact();

        return map.put( mildKey( key ), value );
    }

    public final void putAll( final Map<? extends K, ? extends V> m )
    {
        compact();

        for ( final Entry<? extends K, ? extends V> e : m.entrySet() )
        {
            map.put( mildKey( e.getKey() ), e.getValue() );
        }
    }

    public final V remove( final Object key )
    {
        compact();

        return map.remove( tempKey( key ) );
    }

    public final void clear()
    {
        map.clear();

        compact();
    }

    public final boolean isEmpty()
    {
        compact();

        return map.isEmpty();
    }

    public final int size()
    {
        compact();

        return map.size();
    }

    /**
     * WARNING: this view is a snapshot; updates to it are <em>not</em> reflected in the original map, or vice-versa.
     * <hr>
     * {@inheritDoc}
     */
    public final Set<K> keySet()
    {
        compact();

        final Set<K> keys = new HashSet<K>();
        for ( final Reference<K> r : map.keySet() )
        {
            final K key = r.get();
            if ( null != key )
            {
                keys.add( key );
            }
        }
        return keys;
    }

    public final Collection<V> values()
    {
        compact();

        return map.values();
    }

    /**
     * WARNING: this view is a snapshot; updates to it are <em>not</em> reflected in the original map, or vice-versa.
     * <hr>
     * {@inheritDoc}
     */
    public final Set<Entry<K, V>> entrySet()
    {
        compact();

        final Map<K, V> entries = new HashMap<K, V>();
        for ( final Entry<Reference<K>, V> e : map.entrySet() )
        {
            final K key = e.getKey().get();
            if ( null != key )
            {
                entries.put( key, e.getValue() );
            }
        }
        return entries.entrySet();
    }

    // ----------------------------------------------------------------------
    // Implementation methods
    // ----------------------------------------------------------------------

    /**
     * @return Soft or weak {@link Reference} for the given key.
     */
    final Reference<K> mildKey( final K key )
    {
        return soft ? new Soft<K>( key, queue ) : new Weak<K>( key, queue );
    }

    /**
     * @return Temporary {@link Reference} for the given key; used in queries.
     */
    static final <K> Reference<K> tempKey( final K key )
    {
        return new Weak<K>( key, null );
    }

    /**
     * Compacts the map by removing cleared keys.
     */
    final void compact()
    {
        for ( Reference<? extends K> ref; ( ref = queue.poll() ) != null; )
        {
            map.remove( ref );
        }
    }

    // ----------------------------------------------------------------------
    // Implementation types
    // ----------------------------------------------------------------------

    /**
     * Soft key that maintains a constant hash and uses referential equality.
     */
    static class Soft<T>
        extends SoftReference<T>
    {
        // ----------------------------------------------------------------------
        // Implementation fields
        // ----------------------------------------------------------------------

        private final int hash;

        // ----------------------------------------------------------------------
        // Constructors
        // ----------------------------------------------------------------------

        Soft( final T o, final ReferenceQueue<T> queue )
        {
            super( o, queue );
            hash = o.hashCode();
        }

        // ----------------------------------------------------------------------
        // Public methods
        // ----------------------------------------------------------------------

        @Override
        public final int hashCode()
        {
            return hash;
        }

        @Override
        public final boolean equals( final Object rhs )
        {
            if ( this == rhs )
            {
                return true; // exact same reference
            }
            final T o = get();
            if ( null != o && rhs instanceof Reference<?> )
            {
                // different reference, but same referent
                return o == ( (Reference<?>) rhs ).get();
            }
            return false;
        }
    }

    /**
     * Weak key that maintains a constant hash and uses referential equality.
     */
    static class Weak<T>
        extends WeakReference<T>
    {
        // ----------------------------------------------------------------------
        // Implementation fields
        // ----------------------------------------------------------------------

        private final int hash;

        // ----------------------------------------------------------------------
        // Constructors
        // ----------------------------------------------------------------------

        Weak( final T o, final ReferenceQueue<T> queue )
        {
            super( o, queue );
            hash = o.hashCode();
        }

        // ----------------------------------------------------------------------
        // Public methods
        // ----------------------------------------------------------------------

        @Override
        public final int hashCode()
        {
            return hash;
        }

        @Override
        public final boolean equals( final Object rhs )
        {
            if ( this == rhs )
            {
                return true; // exact same reference
            }
            final T o = get();
            if ( null != o && rhs instanceof Reference<?> )
            {
                // different reference, but same referent
                return o == ( (Reference<?>) rhs ).get();
            }
            return false;
        }
    }
}
