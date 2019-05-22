package com.commercehub.avro.tools.impl;

import com.commercehub.avro.tools.base.BaseAvroIDLToProtocolTransformer;
import org.apache.avro.compiler.idl.Idl;
import org.apache.avro.compiler.idl.ParseException;

import java.io.File;
import java.io.IOException;

class AvroIDLToProtocolTransformerImpl extends BaseAvroIDLToProtocolTransformer {
    @Override
    protected void transformIDLToProtocol(File inputFile, File outputFile, ClassLoader resourceLoader) throws IOException {
        Idl idl = null;
        try {
            idl = new Idl(inputFile, resourceLoader);
            String protoJson = idl.CompilationUnit().toString(true);
            writeProtocolFile(outputFile, protoJson);
        } catch (IOException | ParseException ex) {
            handleException(inputFile, ex);
        } finally {
            if (idl != null) {
                try {
                    idl.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }
}
