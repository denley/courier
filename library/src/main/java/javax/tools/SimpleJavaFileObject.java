/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package javax.tools;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.nio.CharBuffer;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;

/**
 * Provides simple implementations for most methods in JavaFileObject.
 * This class is designed to be subclassed and used as a basis for
 * JavaFileObject implementations.  Subclasses can override the
 * implementation and specification of any method of this class as
 * long as the general contract of JavaFileObject is obeyed.
 *
 * @author Peter von der Ah&eacute;
 * @since 1.6
 */
public class SimpleJavaFileObject implements JavaFileObject {
    /**
     * A URI for this file object.
     */
    protected final URI uri;

    /**
     * The kind of this file object.
     */
    protected final Kind kind;

    /**
     * Construct a SimpleJavaFileObject of the given kind and with the
     * given URI.
     *
     * @param uri  the URI for this file object
     * @param kind the kind of this file object
     */
    protected SimpleJavaFileObject(URI uri, Kind kind) {
        // null checks
        uri.getClass();
        kind.getClass();
        if (uri.getPath() == null)
            throw new IllegalArgumentException("URI must have a path: " + uri);
        this.uri = uri;
        this.kind = kind;
    }

    public URI toUri() {
        return uri;
    }

    public String getName() {
        return toUri().getPath();
    }

    /**
     * This implementation always throws {@linkplain
     * UnsupportedOperationException}.  Subclasses can change this
     * behavior as long as the contract of {@link FileObject} is
     * obeyed.
     */
    public InputStream openInputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * This implementation always throws {@linkplain
     * UnsupportedOperationException}.  Subclasses can change this
     * behavior as long as the contract of {@link FileObject} is
     * obeyed.
     */
    public OutputStream openOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Wraps the result of {@linkplain #getCharContent} in a Reader.
     * Subclasses can change this behavior as long as the contract of
     * {@link FileObject} is obeyed.
     *
     * @param  ignoreEncodingErrors {@inheritDoc}
     * @return a Reader wrapping the result of getCharContent
     * @throws IllegalStateException {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        CharSequence charContent = getCharContent(ignoreEncodingErrors);
        if (charContent == null)
            throw new UnsupportedOperationException();
        if (charContent instanceof CharBuffer) {
            CharBuffer buffer = (CharBuffer)charContent;
            if (buffer.hasArray())
                return new CharArrayReader(buffer.array());
        }
        return new StringReader(charContent.toString());
    }

    /**
     * This implementation always throws {@linkplain
     * UnsupportedOperationException}.  Subclasses can change this
     * behavior as long as the contract of {@link FileObject} is
     * obeyed.
     */
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Wraps the result of openOutputStream in a Writer.  Subclasses
     * can change this behavior as long as the contract of {@link
     * FileObject} is obeyed.
     *
     * @return a Writer wrapping the result of openOutputStream
     * @throws IllegalStateException {@inheritDoc}
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    public Writer openWriter() throws IOException {
        return new OutputStreamWriter(openOutputStream());
    }

    /**
     * This implementation returns {@code 0L}.  Subclasses can change
     * this behavior as long as the contract of {@link FileObject} is
     * obeyed.
     *
     * @return {@code 0L}
     */
    public long getLastModified() {
        return 0L;
    }

    /**
     * This implementation does nothing.  Subclasses can change this
     * behavior as long as the contract of {@link FileObject} is
     * obeyed.
     *
     * @return {@code false}
     */
    public boolean delete() {
        return false;
    }

    /**
     * @return {@code this.kind}
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * This implementation compares the path of its URI to the given
     * simple name.  This method returns true if the given kind is
     * equal to the kind of this object, and if the path is equal to
     * {@code simpleName + kind.extension} or if it ends with {@code
     * "/" + simpleName + kind.extension}.
     *
     * <p>This method calls {@link #getKind} and {@link #toUri} and
     * does not access the fields {@link #uri} and {@link #kind}
     * directly.
     *
     * <p>Subclasses can change this behavior as long as the contract
     * of {@link JavaFileObject} is obeyed.
     */
    public boolean isNameCompatible(String simpleName, Kind kind) {
        String baseName = simpleName + kind.extension;
        return kind.equals(getKind())
            && (baseName.equals(toUri().getPath())
                || toUri().getPath().endsWith("/" + baseName));
    }

    /**
     * This implementation returns {@code null}.  Subclasses can
     * change this behavior as long as the contract of
     * {@link JavaFileObject} is obeyed.
     */
    public NestingKind getNestingKind() { return null; }

    /**
     * This implementation returns {@code null}.  Subclasses can
     * change this behavior as long as the contract of
     * {@link JavaFileObject} is obeyed.
     */
    public Modifier getAccessLevel()  { return null; }

    @Override
    public String toString() {
        return getClass().getName() + "[" + toUri() + "]";
    }
}
