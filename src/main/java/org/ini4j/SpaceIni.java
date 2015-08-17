package org.ini4j;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import javax.swing.JFormattedTextField.AbstractFormatter;

import org.ini4j.spi.IniFormatter;
import org.ini4j.spi.IniParser;

public class SpaceIni extends Ini {
	public SpaceIni(FileReader fileReader) throws InvalidFileFormatException, IOException {
		super(fileReader);
	}

	public SpaceIni(File file) throws InvalidFileFormatException, IOException {
		super(file);
		handler.setOperator(' ');
	}

	@Override public void load(URL input) throws IOException, InvalidFileFormatException
    {
		parser.set_operators(" 	");
        
    }
	
	@Override public void load(Reader input) throws IOException, InvalidFileFormatException
    {
		parser.set_operators(" 	");
		parser.parse(input, handler);
    }

}
