package target.exercise3;

import target.exercise2.File;

public class BreakTypeStateAnalysis {
	File file = new File();
	
	public void test() {
		file.open();	
	}

	public File fileObj()
	{
		File testfile = new File();
		testfile.open();
		File testfilecopy = new File();
		testfilecopy = testfile;
		return testfilecopy;
	}

	public void testFunc1() {
		File file1 = fileObj();
		file1.open();
		file1.close();
	}

	public void testFunc2() {
		File file1 = new File();
		file1.close();
		file1 = fileObj();
		file1.close();
		file1 = new File();
		file1.open();
		file1.close();
	}

}
