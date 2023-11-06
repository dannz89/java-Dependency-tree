package com.ddt.dependencyutils;

import com.ddt.dependencyutils.exception.CircularDependencyException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class DependencyTest
{
	public final static Logger logger = LoggerFactory.getLogger(DependencyTest.class);

	@Test
	public void reAddDependency() throws CircularDependencyException {
		Dependency<Integer, String> d1 = new Dependency<>(1, "d1 data");
		Dependency<Integer, String> d2 = new Dependency<>(2, "d2 data");
		Dependency<Integer, String> d3 = new Dependency<>(3, "d3 data");
		Dependency<Integer, String> d4 = new Dependency<>(4, "d4 data");

		// D4 ultimately depends on D1
		// D4->D1
		//	D2
		//		D3
		//			D4
		//
		d4.addDependency(d3);
		d3.addDependency(d2);
		d2.addDependency(d1);

		d4.setSerializingScheme(DependencyForest.SerializingScheme.DEPENDENCIES);
		logger.debug(d4.treeToString());
		assertEquals(d1, d1);

		// So now we should hurl.
		CircularDependencyException thrown = assertThrows(
				CircularDependencyException.class,
				() -> d1.addDependency(d4));

		assertTrue(thrown != null);
		logger.debug(d4.treeToString());

		// But this should be ok.
		assertDoesNotThrow(() -> d2.addDependency(d1));
	}

	@Test
	public void hasAndGetAndOptionalTests() {
		Dependency<Integer, String> dep1 = new Dependency<>(1, "dep 1");
		Dependency<Integer, String> dep2 = new Dependency<>(1, "dep 1");
		Dependency<Integer, String> dep3 = new Dependency<>(3, "dep 3");

		DependencyForest<Integer, String> forest = new DependencyForest<>();
		forest.addDependency(dep1);
		assertEquals(dep1, dep2);
		forest.addDependency(dep2);
		assertEquals(1, forest.size());
		assertTrue(forest.hasDependency(dep2));
		forest.addDependency(dep3);
		assertEquals(2, forest.size());
		try {
			dep3.addDependency(dep1);
		} catch (Exception e) {
		}
		;
		assertEquals(1, forest.getDependenciesWithNoDependencies().size());
		assertEquals(1, forest.getOutermostLeafDependencies().size());
		Dependency<Integer, String> dep4 = new Dependency<>(4, "Dep 4");
		assertNull(forest.get(dep4));
	}
	@Test
	public void equality() {
		Dependency<String, String> dep1 = new Dependency<>("1", "Dep 1");
		Dependency<String, String> dep2 = new Dependency<>("1", "Dep 1");
		dep1.setFinished(true);
		dep2.setFinished(true);
		assertEquals(dep1, dep2);
		dep2.setFinished(false);
		assertNotEquals(dep1, dep2);
		dep1.setFinished(false);
		assertEquals(dep1, dep2);
	}

	@Test
	public void testEqualsWithSameDataKey() {

		Dependency<String, String> dependency1 = new Dependency<>("One", "Apple");
		Dependency<String, String> dependency2 = new Dependency<>("One", "Bat");
		Dependency<String, String> dRef = dependency1;
		assertNotEquals(dependency1, dependency2);
		assertEquals(dRef, dependency1);
	}

	@Test
	public void whenForestHasSameKeyWithDifferentValue_thenAddAgain() throws CircularDependencyException {
		DependencyForest<String, String> theNewForest = new DependencyForest<>();

		Dependency<String, String> dep1 = new Dependency<>("Dep 1", "I am the original!");
		Dependency<String, String> imposterDep = new Dependency<>("Dep 1", "Imposter.");
		theNewForest.addDependency(dep1);
		theNewForest.addDependency(imposterDep);
		Dependency<String, String> theTruth = theNewForest.getAllNodes().get("Dep 1");
		assertEquals(theTruth, imposterDep);
	}

	@Test
	public void noDuplicateDependenciesAdded() throws CircularDependencyException{
		DependencyForest<String, String> dependencyForest = new DependencyForest<>();

		Dependency<String,String> DependencyA = new Dependency<>("A","Dependency A");
		Dependency<String,String> DependencyB = new Dependency<>("B","Dependency B");
		DependencyB.addDependency(DependencyA);
		DependencyB.addDependency(DependencyA);

		dependencyForest.addDependency(DependencyA);
		dependencyForest.setSerializingScheme(DependencyForest.SerializingScheme.DEPENDANTS);

		dependencyForest.setSerializingScheme(DependencyForest.SerializingScheme.DEPENDENCIES);
		assertEquals(1,DependencyB.getDependencies().size());
		assertEquals(1,DependencyA.getDependants().size());
	}

	@Test
	public void throwAnException() throws CircularDependencyException {
		Dependency<String,String> DependencyA = new Dependency<>("A taks","A Dependency");
		Dependency<String,String> DependencyQ = new Dependency<>("Q Dependency","Q Dependency");
		Dependency<String,String> DependencyR = new Dependency<>("R Dependency","R Dependency");
		Dependency<String,String> DependencyS = new Dependency<>("S Dependency","S Dependency");
		Dependency<String,String> DependencyC = new Dependency<>("C Dependency","C Dependency");
		Dependency<String,String> DependencyD = new Dependency<>("D Dependency","D Dependency");
		Dependency<String,String> DependencyE = new Dependency<>("E Dependency","E Dependency");
		Dependency<String,String> DependencyF = new Dependency<>("F Dependency","F Dependency");
		Dependency<String,String> DependencyH = new Dependency<>("H Dependency","H Dependency");
		Dependency<String,String> DependencyX = new Dependency<>("X Dependency","X Dependency");
		Dependency<String,String> DependencyY = new Dependency<>("Y Dependency","Y Dependency");
		Dependency<String,String> DependencyZ = new Dependency<>("Z Dependency","Z Dependency");

		//C             Q               X
		//D ->  A   ->  R   ->  H   ->  Y   ->  A <=== Circular dependency.
		//E             S
		//
		//F -------->   Z

		DependencyA.addDependency(DependencyC);
		DependencyA.addDependency(DependencyD);
		DependencyA.addDependency(DependencyE);

		DependencyZ.addDependency(DependencyF);

		DependencyQ.addDependency(DependencyA);
		DependencyR.addDependency(DependencyA);
		DependencyS.addDependency(DependencyA);

		DependencyH.addDependency(DependencyQ);
		DependencyH.addDependency(DependencyR);
		DependencyH.addDependency(DependencyS);

		DependencyX.addDependency(DependencyH);
		DependencyY.addDependency(DependencyH);

		CircularDependencyException thrown = assertThrows(
				CircularDependencyException.class,
				() -> DependencyA.addDependency(DependencyQ));

		assertTrue(thrown != null);
	}

	@Test
	public void dependantCount() throws CircularDependencyException {
		DependencyForest<String, String> dependencyForest = new DependencyForest<>();
		Dependency<String, String> X = new Dependency<>("X", "X Dependency");
		Dependency<String, String> Y = new Dependency<>("Y", "Y Dependency");
		dependencyForest.addDependency(X);
		dependencyForest.addDependency(Y);
		// Dependant Tree now looks like this:
		//  X
		//  Y
		assertEquals(2, dependencyForest.getDependenciesWithNoDependencies().size());
		assertEquals(2, dependencyForest.getOutermostLeafDependencies().size());
		assertEquals(2, dependencyForest.size());

		X.addDependency(Y);
		// Dependant tree now looks like this:
		//  Y   ->  X
		assertEquals(1, dependencyForest.getDependenciesWithNoDependencies().size());
		assertEquals(1, dependencyForest.getOutermostLeafDependencies().size());
		assertEquals(2, dependencyForest.size());
	}

	@Test
	public void smallJsonToObjects() throws Exception {
		DependencyForest<String, String> dependencyForest = new DependencyForest<>();
		dependencyForest.setSerializingScheme(DependencyForest.SerializingScheme.DEPENDANTS);

		String jsons = "{\"dataKey\":\"X\",\"data\":\"X Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"Y\",\"data\":\"Y Dependency\",\"finished\":false,\"dependencies\":[null]}]}";
		Collection<Dependency> c = Dependency.fromJson(jsons);
		Dependency<String, String> t = ((ArrayList<Dependency>) c).get(0);
		dependencyForest.addDependency(t);

		assertEquals(1,t.getDependencies().size());
		assertEquals(2, dependencyForest.size());
		assertEquals(1, dependencyForest.getDependenciesWithNoDependencies().size());
	}

	@Test
	public void bigJsonInOut() throws Exception {
		DependencyForest<String, String> dependencyForest = new DependencyForest<>();
		dependencyForest.setSerializingScheme(DependencyForest.SerializingScheme.DEPENDENCIES);

		String bigJsons = "[{\"dataKey\":\"Z\",\"data\":\"Z Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"F\",\"data\":\"F Dependency\",\"finished\":false,\"dependencies\":[null]}]},{\"dataKey\":\"X\",\"data\":\"X Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"H\",\"data\":\"H Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"Q\",\"data\":\"Q Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"A\",\"data\":\"A Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"C\",\"data\":\"C Dependency\",\"finished\":false,\"dependencies\":[null]},{\"dataKey\":\"D\",\"data\":\"D Dependency\",\"finished\":false,\"dependencies\":[null]},{\"dataKey\":\"E\",\"data\":\"E Dependency\",\"finished\":false,\"dependencies\":[null]}]}]},{\"dataKey\":\"R\",\"data\":\"R Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"A\",\"data\":\"A Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"C\",\"data\":\"C Dependency\",\"finished\":false,\"dependencies\":[null]},{\"dataKey\":\"D\",\"data\":\"D Dependency\",\"finished\":false,\"dependencies\":[null]},{\"dataKey\":\"E\",\"data\":\"E Dependency\",\"finished\":false,\"dependencies\":[null]}]}]},{\"dataKey\":\"S\",\"data\":\"S Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"A\",\"data\":\"A Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"C\",\"data\":\"C Dependency\",\"finished\":false,\"dependencies\":[null]},{\"dataKey\":\"D\",\"data\":\"D Dependency\",\"finished\":false,\"dependencies\":[null]},{\"dataKey\":\"E\",\"data\":\"E Dependency\",\"finished\":false,\"dependencies\":[null]}]}]}]}]},{\"dataKey\":\"Y\",\"data\":\"Y Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"H\",\"data\":\"H Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"Q\",\"data\":\"Q Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"A\",\"data\":\"A Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"C\",\"data\":\"C Dependency\",\"finished\":false,\"dependencies\":[null]},{\"dataKey\":\"D\",\"data\":\"D Dependency\",\"finished\":false,\"dependencies\":[null]},{\"dataKey\":\"E\",\"data\":\"E Dependency\",\"finished\":false,\"dependencies\":[null]}]}]},{\"dataKey\":\"R\",\"data\":\"R Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"A\",\"data\":\"A Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"C\",\"data\":\"C Dependency\",\"finished\":false,\"dependencies\":[null]},{\"dataKey\":\"D\",\"data\":\"D Dependency\",\"finished\":false,\"dependencies\":[null]},{\"dataKey\":\"E\",\"data\":\"E Dependency\",\"finished\":false,\"dependencies\":[null]}]}]},{\"dataKey\":\"S\",\"data\":\"S Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"A\",\"data\":\"A Dependency\",\"finished\":false,\"dependencies\":[{\"dataKey\":\"C\",\"data\":\"C Dependency\",\"finished\":false,\"dependencies\":[null]},{\"dataKey\":\"D\",\"data\":\"D Dependency\",\"finished\":false,\"dependencies\":[null]},{\"dataKey\":\"E\",\"data\":\"E Dependency\",\"finished\":false,\"dependencies\":[null]}]}]}]}]}]";
		Collection<Dependency> newForest = Dependency.fromJson(bigJsons);

		newForest.forEach(dep -> dependencyForest.addDependency(dep));

		dependencyForest.setSerializingScheme(DependencyForest.SerializingScheme.DEPENDENCIES);

		assertTrue(bigJsons.equals(dependencyForest.toJson()));
	}


	@Test
	public void testAllTreesToStringsCount() throws CircularDependencyException {
		//C             Q               X
		//D ->  A   ->  R   ->  H   ->  Y
		//E             S
		//
		//F ------------------------>   Z
		DependencyForest<String, String> dependencyForest = new DependencyForest<>();

		Dependency<String, String> dependencyA = new Dependency<>("A", "A Dependency");
		Dependency<String, String> dependencyQ = new Dependency<>("Q", "Q Dependency");
		Dependency<String, String> dependencyR = new Dependency<>("R", "R Dependency");
		Dependency<String, String> dependencyS = new Dependency<>("S", "S Dependency");
		Dependency<String, String> dependencyC = new Dependency<>("C", "C Dependency");
		Dependency<String, String> dependencyD = new Dependency<>("D", "D Dependency");
		Dependency<String, String> dependencyE = new Dependency<>("E", "E Dependency");
		Dependency<String, String> dependencyF = new Dependency<>("F", "F Dependency");
		Dependency<String, String> dependencyH = new Dependency<>("H", "H Dependency");
		Dependency<String, String> dependencyX = new Dependency<>("X", "X Dependency");
		Dependency<String, String> dependencyY = new Dependency<>("Y", "Y Dependency");
		Dependency<String, String> dependencyZ = new Dependency<>("Z", "Z Dependency");

		dependencyA.addDependency(dependencyC);
		dependencyA.addDependency(dependencyD);
		dependencyA.addDependency(dependencyE);

		dependencyZ.addDependency(dependencyF);

		dependencyQ.addDependency(dependencyA);
		dependencyR.addDependency(dependencyA);
		dependencyS.addDependency(dependencyA);

		dependencyH.addDependency(dependencyQ);
		dependencyH.addDependency(dependencyR);
		dependencyH.addDependency(dependencyS);

		dependencyX.addDependency(dependencyH);

		dependencyY.addDependency(dependencyH);

		dependencyForest.addDependency(dependencyZ);
		dependencyForest.addDependency(dependencyX);
		dependencyForest.addDependency(dependencyY);

		dependencyForest.updateAllDependencies();

		assertEquals(12, dependencyForest.size());
		assertEquals(3, dependencyForest.getOutermostLeafDependencies().size());
		assertEquals(4, dependencyForest.getDependenciesWithNoDependencies().size());
	}
}