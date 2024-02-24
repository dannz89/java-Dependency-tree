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
	public void checkRootNodes() {
		Dependency<String, String> depZ = new Dependency<>("Z", "dep Z");
		Dependency<String, String> depC = new Dependency<>("C", "dep C");
		Dependency<String, String> depA = new Dependency<>("A", "dep A");
		Dependency<String, String> depB = new Dependency<>("B", "dep B");
		Dependency<String, String> dep1 = new Dependency<>("1", "dep 1");
		Dependency<String, String> dep2 = new Dependency<>("2", "dep 2");
		Dependency<String, String> dep3 = new Dependency<>("3", "dep 3");
		Dependency<String, String> depe = new Dependency<>("e", "dep e");

		try {
			depe.addDependency(dep3);

			dep3.addDependency(dep2);

			dep3.addDependency(dep1);

			dep2.addDependency(depA);
			dep2.addDependency(depB);
			dep2.addDependency(depC);

			dep1.addDependency(depA);
			dep1.addDependency(depB);
			dep1.addDependency(depC);
		} catch (Exception e) {
			logger.error("Exception occurred. ", e);
		}

		depe.getRootNodes().values().forEach(dep -> logger.info("depe Root node: {}", dep.getDataKey()));
		dep3.getRootNodes().values().forEach(dep -> logger.info("dep3 Root node: {}", dep.getDataKey()));
		dep1.getRootNodes().values().forEach(dep -> logger.info("dep1 Root node: {}", dep.getDataKey()));
		depA.getRootNodes().values().forEach(dep -> logger.info("depA Root node: {}", dep.getDataKey()));
		depB.getRootNodes().values().forEach(dep -> logger.info("depB Root node: {}", dep.getDataKey()));
		depC.getRootNodes().values().forEach(dep -> logger.info("depC Root node: {}", dep.getDataKey()));
		depZ.getRootNodes().values().forEach(dep -> logger.info("depZ Root node: {}", dep.getDataKey()));

		assertEquals(1, depZ.getRootNodes().size());
		assertEquals(3, dep3.getRootNodes().size());
		assertEquals(1, depA.getRootNodes().size());
		assertEquals(1, depB.getRootNodes().size());
		assertEquals(1, depC.getRootNodes().size());
		assertEquals(3, dep2.getRootNodes().size());
	}


	@Test
	public void removeADependency() {
		Dependency<String, String> depZ = new Dependency<>("Z", "dep Z");
		Dependency<String, String> depC = new Dependency<>("C", "dep C");
		Dependency<String, String> depA = new Dependency<>("A", "dep A");
		Dependency<String, String> depB = new Dependency<>("B", "dep B");
		Dependency<String, String> dep1 = new Dependency<>("1", "dep 1");
		Dependency<String, String> dep2 = new Dependency<>("2", "dep 2");
		Dependency<String, String> dep3 = new Dependency<>("3", "dep 3");
		Dependency<String, String> depe = new Dependency<>("e", "dep e");
		Dependency<String, String> depf = new Dependency<>("f", "dep f");
		Dependency<String, String> depg = new Dependency<>("g", "dep g");
		Dependency<String, String> deph = new Dependency<>("h", "dep h");
		Dependency<String, String> depJohnny = new Dependency<>("Johnny", "Johnny");
		Dependency<String, String> depDannny = new Dependency<>("Danny", "Danny");
		depA.setSerializingScheme(DependencyForest.SerializingScheme.DEPENDANTS);
		depB.setSerializingScheme(DependencyForest.SerializingScheme.DEPENDANTS);
		dep1.setSerializingScheme(DependencyForest.SerializingScheme.DEPENDANTS);
		dep2.setSerializingScheme(DependencyForest.SerializingScheme.DEPENDANTS);
		dep3.setSerializingScheme(DependencyForest.SerializingScheme.DEPENDANTS);
		depe.setSerializingScheme(DependencyForest.SerializingScheme.DEPENDANTS);
		depf.setSerializingScheme(DependencyForest.SerializingScheme.DEPENDANTS);
		depg.setSerializingScheme(DependencyForest.SerializingScheme.DEPENDANTS);
		deph.setSerializingScheme(DependencyForest.SerializingScheme.DEPENDANTS);

		try {
			// f+
			// g+
			//	|-h
			deph.addDependency(depf);
			deph.addDependency(depg);

			// e+
			//	|-f+
			//	|-g+
			//	   |-h
			depf.addDependency(depe);
			depg.addDependency(depe);


			// 2+
			//	|-e+
			//	   |-f+
			//	   |-g+
			//	      |-h
			depe.addDependency(dep2);

			// 1+
			// 	|2+
			//	  |-e+
			//	     |-f+
			//	     |-g+
			//	        |-h
			dep2.addDependency(dep1);

			//A+
			//B+
			// |1+
			// 	 |2+
			//	   |-e+
			//	      |-f+
			//	      |-g+
			//	         |-h
			dep1.addDependency(depA);
			dep1.addDependency(depB);

			// 3+
			//	|-f+
			//	|-g+
			//	|  |-h
			//  |-Johnny
			//  |-Danny
			depf.addDependency(dep3);
			depg.addDependency(dep3);
			depJohnny.addDependency(dep3);
			depDannny.addDependency(dep3);

			// Whole tree.
			//Z+
			//C
			//A+
			//B+
			// Z|1+
			// Z| |2+
			// Z|   |-e+
			// Z|   |
			// Z|   |
			// Z|   |
			// ----Z|3+
			//     ||-f+
			//	   ||-g+
			//	      |-h
			//	   |-Johnny
			//	   |-Dan

			dep3.addDependency(depA);
			dep3.addDependency(depB);
			dep3.addDependency(depZ);

			assertTrue(depA.hasDependant(dep3));
			assertTrue(depB.hasDependant(dep3));

			logger.info("Dep A size:{}", depA.size());
			logger.info("Dep B size:{}", depB.size());
			logger.info("Dep Z size:{}", depZ.size());

			logger.info(depA.treeToString());
			logger.info(depB.treeToString());
			logger.info(depZ.treeToString());

			depf.removeDependency(dep3);
			depg.removeDependency(dep3);
			assertFalse(depf.hasDependency(dep3));
			assertFalse(depg.hasDependency(dep3));
			assertFalse(depJohnny.hasDependency(dep3));
			assertFalse(depDannny.hasDependency(dep3));

			logger.info("Dep A size now:{}", depA.size());
			logger.info("Dep B size now:{}", depB.size());
			logger.info("Dep Z size now:{}", depZ.size());

			logger.info(depA.treeToString());
			logger.info(depB.treeToString());
			logger.info(depZ.treeToString());
		} catch (Exception e) {
			logger.error("The exception that shouldn't be", e);
		}

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