"""
This module is to determine optimal biomass production share (%) for biofuel production between multiple regions
(i.e., subAreas). the 

Author: Fei Xie
"""


import pyomo.environ as pyo
from pyomo.core.base.PyomoModel import AbstractModel

class Partition_Share_Model(AbstractModel):
    def __init__(self):
        AbstractModel.__init__(self)
        #set
        self.subAreas = pyo.Set() #0, 1, 2,... 10
        self.shareLevels = pyo.Set()  #0, 1, 2, 3, 4, ..., 100
        self._profit_by_share_area = pyo.Param(self.subAreas, self.shareLevels, domain = pyo.Reals)
        self._share = pyo.Param(self.shareLevels, domain = pyo.Reals)    #share for each interval level, self.shareLevels
        self._total_share = pyo.Param(domain = pyo.Reals)      #100
        
        self.Share_Implement = pyo.Var(self.subAreas, self.shareLevels, domain = pyo.Binary)
        
        def obj_expression(m):
            return sum(sum(m._profit_by_share_area[i,j] * m.Share_Implement[i,j] for j in m.shareLevels) for i in m.subAreas)
        
        self.OBJ = pyo.Objective(rule = obj_expression, sense = pyo.maximize)
        
        @self.Constraint(self.subAreas)
        def constraint_limit_one_share(m, i):
            return sum(m.Share_Implement[i,j] for j in m.shareLevels) == 1
        
        @self.Constraint()
        def constraint_total_share_100(m):
            return sum(sum(m._share[j] * m.Share_Implement[i,j] for j in m.shareLevels) for i in m.subAreas) == m._total_share
        
    def generate_instance(self, input_dict):
        #opt = pyo.SolverFactory(solver)
        instance = self.create_instance(input_dict)
        return instance
    
    def solve_instance(self, solver, instance):
        opt = pyo.SolverFactory(solver)
        results = opt.solve(instance, tee=True)
        return results
       
    def get_optimal_share_dict(self, instance):
        ans = {}
        for i in instance.subAreas:
            for j in instance.shareLevels:
                if instance.Share_Implement[i,j].value == 1:
                    ans[i] = instance._share[j] / 100.0
        return ans
                        