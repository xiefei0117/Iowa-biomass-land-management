from Farmers_Model import Farmers_Model
from Partition_Share_Model import Partition_Share_Model
from pyomo.opt import SolverStatus, TerminationCondition


class Fuelshed_Model:
    def __init__(self, name, sustainability, total_demand, segments, levels, solver):
        self.name = name # scenario name
        self.production = total_demand  #total biofuel demand, 30000000 default
        self.segments = segments         #num of segments
        self.levels = levels       #101, 0%, 1%, ..., 100% 
        self.sustainability = sustainability      #dictionary containing sustainability cost by indicators
        self.subArea_dict = {}
        self.optimal_share_dict = {}              #intermediate results, storing optimal production shares between subArea
        self.solver = solver                      #solver for optimization, cplex, gurobi, etc.
        for segment in range(0, self.segments):
            self.subArea_dict[segment] = {}
            for i in range(0, self.levels):
                self.subArea_dict[segment][i] = -10000
    
    #the function for the entire running process
    def solve(self):
        #calculate memorization table storing obj values by share level for each subArea withe Farmers_Model
        self.calculate_allSubAreas()
        #Determine optimal shares between subArea with the Partition_Share_Model
        input_dict = self.create_pyomo_input_dict()
        print(input_dict)
        self.optimal_share_dict = self.solve_get_optimal_share(input_dict)
        #Re-run each subArea with the optimal share, and output results
        self.post_solve_all_subAreas_print_results(self.optimal_share_dict)
    
    
    def calculate_allSubAreas(self):
        #seg is integer
        for seg in range(0, self.segments):
            self.calculate_subArea(seg)
    
    #calculate for each subArea segment (integer indexed)
    def calculate_subArea(self, segment):
        print("calculating segment: " + str(segment))
        model = Farmers_Model()
        instance = model.generate_instance("inputs/data_" + str(segment) + ".json")
        model.update_sustainability_cost(instance, self.sustainability)
        for i in range(0, self.levels):
            #for each level i in [0 - 100], update biofuel demand of i% of total production volume 
            model.update_biofuel_demand(instance, float(i) / (self.levels - 1) * self.production)
            results = model.solve_instance(self.solver, instance)
            #if infeasible for demand level i% of biofuel demand, then break the loop
            if (results.solver.termination_condition == TerminationCondition.infeasible):
                break
            self.subArea_dict[segment][i] = model.get_OBJ_value(instance)
            
    def create_pyomo_input_dict(self):
        input_dict = {}
        input_dict["subAreas"] = {None : list(range(0, self.segments))}
        input_dict["shareLevels"] = {None : list(range(0, self.levels))}
        #_profit_by_share_area
        temp_profit = {}
        for i in range(0, self.segments):
            for j in range(0, self.levels):
                temp_profit[(i,j)] = self.subArea_dict[i][j];
        input_dict["_profit_by_share_area"] = temp_profit        
        
        #_share
        temp_share = {}
        for i in range(0, self.levels):
            temp_share[i] = float(i)
        input_dict["_share"] = temp_share
        
        input_dict["_total_share"] = {None : float(self.segments * 10)}
        
        return {None : input_dict}
    
    def solve_get_optimal_share(self, input_dictionary):
        print("determine optimal share...")
        model = Partition_Share_Model()
        instance = model.generate_instance(input_dictionary)
        results = model.solve_instance(self.solver, instance)
        return model.get_optimal_share_dict(instance)
    
    def post_solve_all_subAreas_print_results(self, optimal_share_dict):
        outF = open("outputs/" + self.name + "_results.csv", "w")
        outF.write("CLU,Area,Scenario1,Share1,Scenario2,Share2,Scenario3,Share3,Corn_Ton,Soy_Ton,Stover_Ton,SWG_Ton,sci,watereros,winderos,ave_dsoc,ave_n2o_fl,ave_ch4_fl,ave_nh3_vo,ave_no3_le\n")
        outF.close()
        for key in optimal_share_dict:
            self.post_solve_subArea_print_results(key, float(optimal_share_dict[key]))
    
    
    def post_solve_subArea_print_results(self, segment, share):
        model = Farmers_Model()
        instance = model.generate_instance("inputs/data_" + str(segment) + ".json")
        model.update_sustainability_cost(instance, self.sustainability)
        model.update_biofuel_demand(instance, share * self.production)
        results = model.solve_instance(self.solver, instance)
        model.print_results(instance, self.name)