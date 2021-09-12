from Farmers_Model import Farmers_Model
from Fuelshed_Model import Fuelshed_Model


sustainability_cost = {"sci" : 0.0,
                       "watereros" : 0.0,
                       "winderos" : 0.0,
                       "ave_dsoc" : 0,     #maximize
                       "ave_n2o_fl" : 0.0,
                       "ave_ch4_fl" : 0,
                       "ave_nh3_vo" : 0.0,
                       "ave_no3_le" : 0.0
                       }

biofuel_demand = 30000000

Nevada = Fuelshed_Model("test", sustainability_cost, biofuel_demand, 10, 101, "cplex")
Nevada.solve()

