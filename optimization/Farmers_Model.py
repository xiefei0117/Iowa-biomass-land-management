"""
This module is to determine the optimal farm land managment (50 scenarios per farm) for specific area (i.e., subArea)
considering both profits to farmers and the sustainability impacts to the area.

Author: Fei Xie
"""

from __future__ import division
import pyomo.environ as pyo
from pyomo.core.base.PyomoModel import AbstractModel



class Farmers_Model(AbstractModel):
    def __init__(self):
        AbstractModel.__init__(self)
        #set
        self.FIELDS = pyo.Set()
        self.SCENARIOS = pyo.Set()
        self.PRODS = pyo.Set()
        self.SUSTAINABILITY = pyo.Set()
        #parameters
        self._area = pyo.Param(self.FIELDS, domain = pyo.NonNegativeReals) #acre
        self._unit_cost_scenario = pyo.Param(self.SCENARIOS, domain = pyo.NonNegativeReals) #$/acre
        self._inconvience_cost = pyo.Param(domain = pyo.NonNegativeReals) #$/acre
        self._yield = pyo.Param(self.FIELDS, self.SCENARIOS, self.PRODS, domain = pyo.Reals) #tons
        self._price_for_biofuel = pyo.Param(self.PRODS, domain = pyo.NonNegativeReals) #$/tons
        self._price_for_other = pyo.Param(self.PRODS, domain = pyo.NonNegativeReals) #$/tons
        self._conversion_rate = pyo.Param(self.PRODS, domain = pyo.NonNegativeReals) #gallons/ton
        self._biofuel_demand = pyo.Param(domain = pyo.NonNegativeReals, mutable = True) #gallons
        self._sustainability = pyo.Param(self.FIELDS, self.SCENARIOS, self.SUSTAINABILITY, domain = pyo.Reals)
        self._sustainability_cost = pyo.Param(self.SUSTAINABILITY, domain = pyo.Reals, mutable = True) #$/tons additional subsidy for product for biofuel production
        #decisions
        self.Scenario_Implement = pyo.Var(self.FIELDS, self.SCENARIOS, bounds = (0, 1), domain = pyo.NonNegativeReals) #if scenario is chosen
        self.Production_for_biofuel = pyo.Var(self.FIELDS, self.PRODS, domain = pyo.NonNegativeReals)
        self.Production_for_other = pyo.Var(self.FIELDS, self.PRODS, domain = pyo.NonNegativeReals)
        

        def obj_expression(m):
            return sum(sum(m._price_for_biofuel[k] * m.Production_for_biofuel[i,k] + m._price_for_other[k] * m.Production_for_other[i,k] for k in m.PRODS) - 
                       sum(m._unit_cost_scenario[s] * m._area[i] * m.Scenario_Implement[i,s] for s in m.SCENARIOS) for i in m.FIELDS) + sum(sum(sum(m._sustainability_cost[n] * m._sustainability[i,s,n] for n in m.SUSTAINABILITY) * m._area[i] * m.Scenario_Implement[i,s] for s in m.SCENARIOS) for i in m.FIELDS)
        
        self.OBJ = pyo.Objective(rule = obj_expression, sense = pyo.maximize)
        
        @self.Constraint(self.FIELDS)
        def constraint_non_negative_profit(m, i):
            return sum(m._price_for_biofuel[k] * m.Production_for_biofuel[i,k] + 
                       m._price_for_other[k] * m.Production_for_other[i,k] for k in m.PRODS) - sum(m._unit_cost_scenario[s] * m._area[i] * m.Scenario_Implement[i,s] for s in m.SCENARIOS) >= 0
        
        @self.Constraint(self.FIELDS)
        def constraint_select_scenario(m, i):
            return sum(m.Scenario_Implement[i, s] for s in m.SCENARIOS) == 1
        
        @self.Constraint(self.FIELDS, self.PRODS)
        def constraint_split_production(m, i, k):
            return m.Production_for_biofuel[i, k] + m.Production_for_other[i, k] == sum (m._yield[i, s, k] * m.Scenario_Implement[i, s] for s in m.SCENARIOS)
        
        @self.Constraint()
        def constraint_total_fuel_demand(m):
            return sum(sum(m._conversion_rate[k] * m.Production_for_biofuel[i, k] for k in m.PRODS) for i in m.FIELDS) == m._biofuel_demand
        
    
    def generate_instance(self, data_file):
        data = pyo.DataPortal()
        data.load(filename=data_file, model=self)
        instance = self.create_instance(data)
        return instance
    
    def solve_instance(self, solver, instance):
        opt = pyo.SolverFactory(solver)
        results = opt.solve(instance, tee=True)
        return results
    
    def update_sustainability_cost(self, instance, dict_sustainability_cost):
        print(dict_sustainability_cost)
        for i in instance.SUSTAINABILITY:
            print(i)
            instance._sustainability_cost[i] = dict_sustainability_cost[i]
            
    def update_biofuel_demand(self, instance, production):
        instance._biofuel_demand = production
        
    def get_OBJ_value(self, instance):
        return pyo.value(instance.OBJ)
    
    # will be removed
    '''
    def print_all_results_helper(self, instance, name):
        outF = open("outputs/" + name + "_results.csv", "w")
        outF.write("CLU,Area,Scenario1,Share1,Scenario2,Share2,Scenario3,Share3,Corn_Ton,Soy_Ton,Stover_Ton,SWG_Ton,sci,watereros,winderos,ave_dsoc,ave_n2o_fl,ave_ch4_fl,ave_nh3_vo,ave_no3_le\n")
        self.print_results(instance,outF)
        outF.close()
    '''    
        
    def print_results(self, instance, name):
        outF = open("outputs/" + name + "_results.csv", "a")
        for i in instance.FIELDS:
            #CLU, Area
            outF.write(i[3:] + "," + str(instance._area[i]))
            #,cenario1, Share1, Scenario2, Share2, Scenario3, Share3
            count = 0

            for j in instance.SCENARIOS:
                if count == 3:
                    break
                if (instance.Scenario_Implement[i,j].value > 0):
                    count+=1
                    outF.write("," + j + "," + str(instance.Scenario_Implement[i,j].value))
            
            while count < 3:
                count+=1
                outF.write(",,")
            #,Corn_Ton, Soy_Ton, Stover_Ton, SWG_Ton
            for k in instance.PRODS:
                outF.write("," + str(instance.Production_for_biofuel[i,k].value + instance.Production_for_other[i,k].value))
            
            #,sci,watereros,winderos,ave_dsoc,ave_n2o_fl,ave_ch4_fl,ave_nh3_vo,ave_no3_le
            for n in instance.SUSTAINABILITY:
                outF.write("," + str(sum(instance._sustainability[i,s,n] * \
                                         instance.Scenario_Implement[i, s].value for s in instance.SCENARIOS)))
            outF.write("\n")
        outF.close()

        
            
    def print_decision_results(self, instance, name):
        
        #instance.pprint(filename="outputs/" + name + ".txt")
        
        outF = open("outputs/" + name + "_land_management.csv", "w")
        outF.write("CLU,Scenario,Share\n");
        for i in instance.FIELDS:
            for j in instance.SCENARIOS:
                print(instance.Scenario_Implement[i,j].value)
                if (instance.Scenario_Implement[i,j].value > 0):
                    outF.write(i + "," + j + "," + str(instance.Scenario_Implement[i,j].value) + "\n")
            
        outF.write("\n")
'''
    def get_summary(self, instance):
        print("Total systems cost: " + value(instance.obj)
'''