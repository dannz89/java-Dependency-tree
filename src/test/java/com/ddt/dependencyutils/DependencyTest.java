package com.ddt.dependencyutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ddt.dependencyutils.exception.CircularDependencyException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;

public class DependencyTest
{
	// Your test cases will go here.
	@Test
	public void testEqualsWithSameDataKey() {
		Dependency.clearDependencyTrees();
		Dependency<String,String> Dependency1 = new Dependency<>("One","Apple");
		Dependency<String,String> Dependency2 = new Dependency<>("Two","Bat");
		Dependency<String,String> Dependency3 = new Dependency<>("Three","Cat");
		try {
			Dependency3.addDependency(Dependency1);
			Dependency3.addDependency(Dependency2);
		} catch (Exception e){e.printStackTrace();}

		assertEquals(2,Dependency3.getDependencies().size());
	}

	@Test
	public void noDuplicateDependenciesAdded() throws CircularDependencyException{
		Dependency.clearDependencyTrees();
		Dependency<String,String> DependencyA = new Dependency<>("A","Dependency A");
		Dependency<String,String> DependencyB = new Dependency<>("B","Dependency B");
		DependencyB.addDependency(DependencyA);
		DependencyB.addDependency(DependencyA);
		System.out.println("===> DEPENDANTS IN DUPLICATE TEST <===");
		Dependency.setSerializeDependencies(false);
		Dependency.getDependenciesWithNoDependencies().forEach(Dependency -> System.out.println(Dependency.toJson()));
		System.out.println("===> DEPENDENCIES IN DUPLICATE TEST <===");
		Dependency.setSerializeDependencies(true);
		Dependency.getOutermostLeafDependencies().forEach(Dependency -> System.out.println(Dependency.toJson()));
		assertEquals(1,DependencyB.getDependencies().size());
		assertEquals(1,DependencyA.getDependants().size());
	}

	@Test
	public void throwAnException() throws CircularDependencyException {
		Dependency.clearDependencyTrees();
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

		assertTrue(thrown instanceof CircularDependencyException);
	}

	@Test
	public void dependantCount() throws CircularDependencyException {
		Dependency.clearDependencyTrees();
		Dependency X = new Dependency("X","X Dependency");
		Dependency Y = new Dependency("Y","Y Dependency");
		// Dependant Tree now looks like this:
		//  X
		//  Y
		assertEquals(2,Dependency.getDependenciesWithNoDependencies().size());
		Dependency.setSerializeDependencies(false);
		System.out.println("\n====> PRINTING WHOLE DEPENDANT TREE <====\n");
		for(Dependency<?,?>t:Dependency.getDependenciesWithNoDependencies()){
			System.out.println(t.toJson());
		}
		X.addDependency(Y);
		// Dependant tree now looks like this:
		//  Y   ->  X

		for(Dependency<?,?>t:Dependency.getDependenciesWithNoDependencies()){
			System.out.println(t.toJson());
		}

		System.out.println("\n====> PRINTING WHOLE DEPENDANT TREE <====\n");
		for(Dependency<?,?>t:Dependency.getDependenciesWithNoDependencies()){
			System.out.println(t.toJson());
		}

		System.out.println("\n====> PRINTING WHOLE DEPENDENCY TREE <====\n");
		Dependency.setSerializeDependencies(true);
		for(Dependency<?,?>t:Dependency.getOutermostLeafDependencies()){
			System.out.println(t.toJson());
		}

		assertEquals(1,Dependency.getDependenciesWithNoDependencies().size());
	}

	@Test
	public void fromJsonTest() throws Exception {
		Dependency.clearDependencyTrees();
		String jsons = "{\"dataKey\":\"X\",\"data\":\"X Dependency\",\"finished\":false,\"dependencies\":{\"Y\":{\"dataKey\":\"Y\",\"data\":\"Y Dependency\",\"finished\":false,\"dependencies\":null}}}";
		Dependency t = Dependency.fromJson(jsons);
		Dependency.setSerializeDependencies(true);
		System.out.println("====> GENERATED OBJECTS FROM JSON <====");
		Dependency.getOutermostLeafDependencies().forEach(Dependency -> System.out.println(Dependency.toJson()));
		assertEquals(1,t.getDependencies().size());
	}


	@Test
	public void testAllTreesToStringsCount() throws CircularDependencyException {
		//C             Q               X
		//D ->  A   ->  R   ->  H   ->  Y
		//E             S
		//
		//F ------------------------>   Z
		Dependency.clearDependencyTrees();
		Dependency<String,String> DependencyA = new Dependency<>("A","A Dependency");
		Dependency<String,String> DependencyQ = new Dependency<>("Q","Q Dependency");
		Dependency<String,String> DependencyR = new Dependency<>("R","R Dependency");
		Dependency<String,String> DependencyS = new Dependency<>("S","S Dependency");
		Dependency<String,String> DependencyC = new Dependency<>("C","C Dependency");
		Dependency<String,String> DependencyD = new Dependency<>("D","D Dependency");
		Dependency<String,String> DependencyE = new Dependency<>("E","E Dependency");
		Dependency<String,String> DependencyF = new Dependency<>("F","F Dependency");
		Dependency<String,String> DependencyH = new Dependency<>("H","H Dependency");
		Dependency<String,String> DependencyX = new Dependency<>("X","X Dependency");
		Dependency<String,String> DependencyY = new Dependency<>("Y","Y Dependency");
		Dependency<String,String> DependencyZ = new Dependency<>("Z","Z Dependency");

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

		System.out.println("\n===> PRINTING WHOLE DEPENDENCY TREE <===");
		Dependency.setSerializeDependencies(true);
		Dependency.getOutermostLeafDependencies().forEach(Dependency -> System.out.println(Dependency.toJson()));


		System.out.println("\n====> PRINTING WHOLE DEPENDANT TREE <====\n");
		Dependency.setSerializeDependencies(false);
		Dependency.getDependenciesWithNoDependencies().forEach(Dependency -> System.out.println(Dependency.toJson()));

		assertEquals(3,Dependency.allTreesToStrings().size());
	}
}