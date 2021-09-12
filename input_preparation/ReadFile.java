import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javafx.util.Pair;

public class ReadFile {
	Set<String> scenarioSet;
	Set<Long> noStoverRemovalFarmer;
	Map<String, Float> scenarioUnitCost;
	Map<Long, Farmer> farmerMap;
	Long[] farmerArray;
	String[] productNames;
	String[] sustainabilityNames;
	float[] conversionRate;
	
	float cornTonPerBushel;     //stover and corn has the same weight per bushel
	float soyTonPerBushel;
	float stoverYield30;
	float stoverYield45;
	float stoverYield70;
	float cornPrice;         //$/ton
	float soyPrice;
	float switchGrassPrice; //$/ton
	float stoverPrice;     //$/ton
	
	
	public ReadFile() {
		this.scenarioSet = new HashSet<>();
		this.farmerMap = new HashMap<>();
		this.noStoverRemovalFarmer = new HashSet<>();
		this.scenarioUnitCost = new HashMap<>();
		this.productNames = new String[]{"Corn", "Soy", "Stover", "Switchgrass"};
		this.sustainabilityNames = new String[] {"sci","watereros","winderos","ave_dsoc","ave_n2o_fl","ave_ch4_fl","ave_nh3_vo","ave_no3_le"};
		this.conversionRate = new float[] {0.0f, 0.0f, 85.0f, 85.0f}; //Billion ton 2016, Table A-1
		this.cornTonPerBushel = 0.0237f;  //Billion ton 2016, appendix Table c-3
		this.soyTonPerBushel = 0.0241f; //Billion ton 2016, appendix Table c-3
		
		this.stoverYield30 = 0.225f;  //AntaresResults_for8NevadaCnties_BioSTAR.xlsx, interpreted from corn stover / corn yield;
		this.stoverYield45 = 0.3375f;
		this.stoverYield70 = 0.525f;
		this.cornPrice = 147.68f;   //Billion ton 2016, Table C-9
		this.soyPrice = 365.15f;    //Billon ton 2016, Table C-9
		this.switchGrassPrice = 114.0f; //Tyner's paper
		this.stoverPrice = 57.1f; //https://farmdocdaily.illinois.edu/2016/02/to-harvest-stover-or-not-is-it-worth-it.html
		
		this.readScenarioSet();
		readNoStoverRemovalFarmerSet();
		this.readFarmerMap();
		
		
		for (String scenario : this.scenarioSet) {
			this.readSingleScenario(scenario);
			this.printScenario(scenario);
		}

		
		for (int i = 0; i < 10; i++) {
			printJsonSeperate("data_" + i, i, 100);
		}
		
		
	}
	
	public void printJsonSeperate(String name, int part, int segments) {
		if (this.farmerArray == null)
			return;
		try {
			File outFile = new File("src/output/" + name + ".json");
			FileWriter fWriter = new FileWriter (outFile);
			PrintWriter pWriter = new PrintWriter(fWriter);
			
			int farmerCount = this.farmerArray.length;
			int leftIndex = farmerCount * part / segments;
			int rightBound = Math.min(farmerCount * (part + 1) / segments, farmerCount);
			Long[] fieldIDs = Arrays.copyOfRange(this.farmerArray, leftIndex, rightBound);


			String[] scenarioNames = this.scenarioSet.toArray(new String[this.scenarioSet.size()]);
			
			String tab = "    ";
			pWriter.print("{\n");
			
			//print FIELDS
			System.out.println("print FIELDS");
			pWriter.print(tab + "\"FIELDS\" : [");
			for (int i = 0; i < fieldIDs.length - 1; i++) {
				pWriter.print("\"clu" + fieldIDs[i] + "\",");
				if (i % 15 == 0)
					pWriter.print("\n" + tab + tab);
			}
			pWriter.print("\"clu" + fieldIDs[fieldIDs.length - 1] + "\"],\n");
			
			//print scenarios
			System.out.println("print scenarios");
			pWriter.print(tab + "\"SCENARIOS\" : [");
			for (int i = 0; i < scenarioNames.length - 1; i++) {
				pWriter.print("\"" + scenarioNames[i] + "\",");
				if (i % 10 == 0)
					pWriter.print("\n" + tab + tab);
			}
			pWriter.print("\"" + scenarioNames[scenarioNames.length - 1] + "\"],\n");
			
			//print PRODS
			System.out.println("print PRODS");
			pWriter.print(tab + "\"PRODS\" : [");
			for (int i = 0; i < this.productNames.length - 1; i++) {
				pWriter.print("\"" + this.productNames[i] + "\",");
			}
			pWriter.print("\"" + this.productNames[this.productNames.length - 1] + "\"],\n");
			
			//print SUSTAINABILITY
			System.out.println("print SUSTAINABILITY");
			pWriter.print(tab + "\"SUSTAINABILITY\" : [");
			for (int i = 0; i < this.sustainabilityNames.length - 1; i++) {
				pWriter.print("\"" + this.sustainabilityNames[i] + "\",");
			}
			pWriter.print("\"" + this.sustainabilityNames[this.sustainabilityNames.length - 1] + "\"],\n");
			
			//print _Area
			System.out.println("print _area");
			pWriter.print(tab + "\"_area\" : {");
			for (int i = 0; i < fieldIDs.length - 1; i++) {
				pWriter.print("\"clu" + fieldIDs[i] + "\" : " + this.farmerMap.get(fieldIDs[i]).area + ",");
				if (i % 5 == 0)
					pWriter.print("\n" + tab + tab);
			}
			pWriter.print("\"clu" + fieldIDs[fieldIDs.length - 1] + "\" : " + 
					this.farmerMap.get(fieldIDs[fieldIDs.length - 1]).area + "},\n");
			
			//print _unit_cost_scenario
			System.out.println("print _unit_cost_scenario");
			pWriter.print(tab + "\"_unit_cost_scenario\" : {");
			for (int i = 0; i < scenarioNames.length - 1; i++) {
				pWriter.print("\"" + scenarioNames[i] + "\" : " + this.scenarioUnitCost.get(scenarioNames[i]) + ",");
				if (i % 10 == 0)
					pWriter.print("\n" + tab + tab);
			}
			pWriter.print("\"" + scenarioNames[scenarioNames.length - 1] + "\" : " + 
					this.scenarioUnitCost.get(scenarioNames[scenarioNames.length - 1] ) + "},\n");
			
			//print _yield
			System.out.println("print _yield");
			pWriter.print(tab + "\"_yield\" : [\n");
			int count = fieldIDs.length * scenarioNames.length * this.productNames.length - 1;
			for (int i = 0; i < fieldIDs.length; i++) {
				for (int j = 0; j < scenarioNames.length; j++) {
					for (int k = 0; k < this.productNames.length; k++) {
						float yieldValue = this.farmerMap.get(fieldIDs[i]).yields.get(scenarioNames[j])[k] * this.farmerMap.get(fieldIDs[i]).area;
						if (k == 2 && this.noStoverRemovalFarmer.contains(fieldIDs[i])) {
							System.out.println(fieldIDs[i]);
							yieldValue = -10000;
						}
						pWriter.print(tab + tab + "{\"index\" : [\"clu" + fieldIDs[i] + "\", \"" +
								scenarioNames[j] + "\", \"" + this.productNames[k] + "\"], \"value\" : " + 
								yieldValue + "}");
						if (count > 0)
							pWriter.print(",\n");
						count--;
					}
				}
			}			
			pWriter.print(tab + "\n],\n");
			
			
			
			//print _sustainability
			System.out.println("print _sustainability");
			pWriter.print(tab + "\"_sustainability\" : [\n");
			count = fieldIDs.length * scenarioNames.length * this.sustainabilityNames.length - 1;
			for (int i = 0; i < fieldIDs.length; i++) {
				for (int j = 0; j < scenarioNames.length; j++) {
					for (int k = 0; k < this.sustainabilityNames.length; k++) {
						pWriter.print(tab + tab + "{\"index\" : [\"clu" + fieldIDs[i] + "\", \"" +
								scenarioNames[j] + "\", \"" + this.sustainabilityNames[k] + "\"], \"value\" : " + 
								this.farmerMap.get(fieldIDs[i]).sustainability.get(scenarioNames[j])[k] + "}");
						if (count > 0)
							pWriter.print(",\n");
						count--;
					}
				}
			}
			pWriter.print(tab + "\n],\n");
			
			//print _price
			System.out.println("print _price");
			pWriter.print(tab + "\"_price_for_biofuel\" : {"
					+ "\"" + this.productNames[0] + "\" : " + this.cornPrice + "," + 
					"\"" + this.productNames[1] + "\" : " + this.soyPrice + ",\n" +
					tab + tab + "\"" + this.productNames[2] + "\" : " + this.stoverPrice + "," +
					"\"" + this.productNames[3] + "\" : " + this.switchGrassPrice + "},\n");
			
			pWriter.print(tab + "\"_price_for_other\" : {"
					+ "\"" + this.productNames[0] + "\" : " + this.cornPrice + "," + 
					"\"" + this.productNames[1] + "\" : " + this.soyPrice + ",\n" +
					tab + tab + "\"" + this.productNames[2] + "\" : " + 0 + "," +
					"\"" + this.productNames[3] + "\" : " + 0 + "},\n");
			
			//print _conversion_rate
			System.out.println("print _conversion_rate");
			pWriter.print(tab + "\"_conversion_rate\" : {" +
					"\"" + this.productNames[0] + "\" : " + this.conversionRate[0] + "," +
					"\"" + this.productNames[1] + "\" : " + this.conversionRate[1] + ",\n" +
					tab + tab + "\"" + this.productNames[2] + "\" : " + this.conversionRate[2] + "," +
					"\"" + this.productNames[3] + "\" : " + this.conversionRate[3] + "},\n");
			
			//print _biofuel_demand
			System.out.println("print _biofuel_demand");
			pWriter.print(tab + "\"_biofuel_demand\" : " + 30000000 + ",\n");  //30 Million gallons
			
			//print _sustainability_cost
			System.out.println("print _sustainability_cost");
			pWriter.print(tab + "\"_sustainability_cost\" : {");
			for (int k = 0; k < this.sustainabilityNames.length; k++) {
				pWriter.print("\"" + this.sustainabilityNames[k] + "\" : 0.0");
				if (k < this.sustainabilityNames.length - 1)
					pWriter.print(",");
				if ((k + 1) % 6 == 0)
					pWriter.print("\n" + tab + tab);
			}
			pWriter.print(tab + "}\n");
			pWriter.print("}\n");
			pWriter.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	
	
	
	public void readScenarioSet() {
		try {
			Scanner input = new Scanner(new File("src/input/scenarioCases/unit_cost_land_use_scenario.csv"));
			input.nextLine();
			while (input.hasNextLine()) {
				String[] items = input.nextLine().split(",");
				this.scenarioSet.add(items[0]);
				this.scenarioUnitCost.put(items[0], Float.valueOf(items[1]));
			}
			input.close();
			//System.out.println(this.scenarioSet.size() + " " + this.scenarioSet);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void readNoStoverRemovalFarmerSet() {
		System.out.print("read farmer set not for stover removal...");
		try {
			Scanner input = new Scanner(new File("src/input/CLU_noStoverRemoval.txt"));
			input.nextLine();
			while (input.hasNextLine()) {
				String[] items = input.nextLine().split("\\s+");
				long id = Long.valueOf(items[0]);
				this.noStoverRemovalFarmer.add(id);
			}
			
			input.close();
			System.out.print(" done\n" + this.noStoverRemovalFarmer + "\n" + this.noStoverRemovalFarmer.size()+ "\n");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void readFarmerMap() {
		System.out.print("read farmer data...");
		try {
			Scanner input = new Scanner(new File("src/input/scenarioCases/all_subfields.csv"));
			input.nextLine();
			while (input.hasNextLine()) {
				String[] items = input.nextLine().split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
				long id = Long.valueOf(items[1]);
				Farmer curr = this.farmerMap.computeIfAbsent(id, x -> new Farmer(id, this.scenarioSet));
				curr.area += Float.valueOf(items[2]);
			}
			input.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.farmerArray = this.farmerMap.keySet().toArray(new Long[this.farmerMap.size()]);
		System.out.print(" " + this.farmerArray.length + "farmers, done.\n");
		
	}
	
	public void loadExistingScenario(File f, String scenarioName) {
		try {
			Scanner input = new Scanner(f);
			input.nextLine();
			while (input.hasNextLine()) {
				String[] items = input.nextLine().split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
				Farmer farmer = this.farmerMap.get(Long.valueOf(items[1]));

				float[] yieldScenario = farmer.yields.get(scenarioName);
				for (int i = 0; i < this.productNames.length; i++) {
					yieldScenario[i] = Float.valueOf(items[11 + i]);
				}
				
				float[] sustainabilityScenario = farmer.sustainability.get(scenarioName);
				for (int i = 0; i < sustainabilityScenario.length; i++) {
					sustainabilityScenario[i] = Float.valueOf(items[3 + i]);
				}
			}
			input.close();
			
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void readSingleScenario(String scenarioName) {
		File f = new File("src/output/scenarioCases/" + scenarioName + "_farmers.csv");
		if (f.exists() && !f.isDirectory()) {
			System.out.println("load existing scenario file : " + scenarioName + " ...");
			loadExistingScenario(f, scenarioName);
			return;
		}
		System.out.print("read scenario file : " + scenarioName + " ...");
		try {
			Scanner input = new Scanner(new File("src/input/scenarioCases/" + scenarioName + "_compact.csv"));
			input.nextLine();
			/*string
			 * 0 - IDclumu,
			 * 1 - cluid,
			 * 2 - clumuacres,
			 * 3 - sci,
			 * 4 - watereros,
			 * 5 - winderos,
			 * 6 - ave_dsoc,
			 * 7 - ave_n2o_fl,
			 * 8 - ave_ch4_fl,
			 * 9 - ave_nh3_vo,
			 * 10 - ave_no3_le,
			 * 11 - rotation,
			 * 12 - yields
			 */
			while (input.hasNextLine()) {
				String[] items = input.nextLine().split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
				Farmer farmer = this.farmerMap.get(Long.valueOf(items[1]));
				float fieldArea = Float.valueOf(items[2]);
				float weight = fieldArea / farmer.area;
				
				if (scenarioName.equals("SWG_results")) {
					farmer.yields.get(scenarioName)[3] = 4.0f;
				}
				
				if (!scenarioName.equals("CRP_results") && !scenarioName.equals("SWG_results")) {
					String rotationStr = items[11].replaceAll("^\"|\"$", "");
					String yieldStr = items[12].replaceAll("^\"|\"$", "");
					String[] rotation = rotationStr.split(",");
					String[] yield = yieldStr.split(",");
					//int cornCount = 0;
					//int soyCount = 0;
					float cornYield = 0;
					float soyYield = 0;
					int index = 0;
					
					for (String str : rotation) {
						//if (farmer.id == 2018475)
							//System.out.print(str + " ");
						while (Float.valueOf(yield[index]) > 1000) {
							index++;
						}
						float currYield = Float.valueOf(yield[index++]);
						if (str.equals("CG")) {
							cornYield += currYield;
							//cornCount++;
						}
						else {
							soyYield += currYield;
							//soyCount++;
						}

						
					}
					cornYield = cornYield / 4.0f * this.cornTonPerBushel;
					soyYield = soyYield / 4.0f * this.soyTonPerBushel;
					
					if (scenarioName.contains("NT")) {
						cornYield *= 0.94f; //7596/8074   https://www.sciencedirect.com/science/article/pii/S0378429015300228
						soyYield *= 0.94f;
					}
					
					
					float[] yieldScenario = farmer.yields.get(scenarioName);
					

					yieldScenario[0] += cornYield * weight;
					yieldScenario[1] += soyYield * weight;
					
					

					
					if (scenarioName.contains("30"))
						yieldScenario[2] += cornYield * this.stoverYield30 * weight;
					else if (scenarioName.contains("45"))
						yieldScenario[2] += cornYield * this.stoverYield45 * weight;
					else if (scenarioName.contains("70"))
						yieldScenario[2] += cornYield * this.stoverYield70 * weight;
				}
				
				//allocate sustainability indicator
				float[] sustainabilityScenario = farmer.sustainability.get(scenarioName);
				for (int i = 0; i < sustainabilityScenario.length; i++) {
					sustainabilityScenario[i] += Float.valueOf(items[3 + i]) * weight;
				}
				
				
				
			}
			input.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.print("done.\n");
		
	}
	
	public void printScenario(String scenarioName) {
		try {
			File outFile = new File("src/output/scenarioCases/" + scenarioName + "_farmers.csv");
			FileWriter fWriter = new FileWriter (outFile);
			PrintWriter pWriter = new PrintWriter(fWriter);
			
			pWriter.print("scenario,cluid,area,sci,watereros,winderos,ave_dsoc,ave_n2o_fl,ave_ch4_fl,ave_nh3_vo,ave_no3_le,corn,soy,stover,switchgrass\n");
			for (Long id : this.farmerMap.keySet()) {
				Farmer farmer = this.farmerMap.get(id);
				pWriter.print(scenarioName + "," + farmer.id + "," + farmer.area);
				
				float[] sustainabilityScenario = farmer.sustainability.get(scenarioName);
				for (int i = 0; i < sustainabilityScenario.length; i++) {
					pWriter.print("," + sustainabilityScenario[i]);
				}
				
				float[] yieldScenario = farmer.yields.get(scenarioName);
				for (int i = 0; i < yieldScenario.length; i++) {
					pWriter.print("," + yieldScenario[i]);
				}
				pWriter.print("\n");
			}
			pWriter.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
