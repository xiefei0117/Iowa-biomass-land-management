import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Farmer {
	public long id;
	public float area;
	
	public Map<String, float[]> yields; //0 - corn, 1 - soy, 2 - corn stover, 3 switchgrass
	public Map<String, float[]> sustainability;
	
	
	
	public Farmer(long id, Set<String> scenarioSet) {
		this.id = id;
		this.area = 0;
		this.yields = new HashMap<>();
		this.sustainability = new HashMap<>();
		
		for (String scenario : scenarioSet) {
			this.yields.put(scenario, new float[4]);
			this.sustainability.put(scenario, new float[8]);
			
			//if (scenario.equals("SWG_results"))
				//this.yields.get(scenario)[3] = this.area * 4.0f;
		}
	}
}
