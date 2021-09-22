'''
This code is to get summary data from calculated results for each scenario
Author: Fei Xie

'''


import pandas as pd
pd.set_option('display.max_columns', None)



def process_results_for_summary(filename):
    print("get summary results for scenario: " + filename)
    sustainability = ['sci', 'watereros', 'winderos', 'ave_dsoc', 'ave_n2o_fl', 'ave_ch4_fl',
       'ave_nh3_vo', 'ave_no3_le']
    product = ['Corn_Ton', 'Soy_Ton', 'Stover_Ton', 'SWG_Ton']
    
    cluid_fips_df = pd.read_csv("cluid_fips.csv")
    cluid_results_df = pd.read_csv(filename + "/" + filename + "_results.csv")
    cluid_results_df = pd.merge(cluid_results_df, cluid_fips_df, left_on = "CLU", right_on = "cluid")

    
    #get sustainability multiply by area
    for indicator in sustainability:
        cluid_results_df[indicator + "_weighted"] = cluid_results_df[indicator] * cluid_results_df["Area"]
    
    #create a list of attributes for the final results    
    final_list = ["fips", "Area"]
    final_list.extend(product)
    for indicator in sustainability:
        final_list.append(indicator + "_weighted")
    
    #group by and sum    
    results_df = cluid_results_df[final_list].groupby(["fips"]).sum().reset_index()
    
    summary_dict = {}
    summary_dict["fips"] = "Summary"
    summary_dict["Area"] = results_df["Area"].sum()
    for item in product:
        summary_dict[item] = results_df[item].sum()
    for indicator in sustainability:
        summary_dict[indicator] = results_df[indicator + "_weighted"].sum() / summary_dict["Area"]
        
    
    #get average sustainability
    for indicator in sustainability:
        results_df = results_df.rename(columns = {indicator + "_weighted" : indicator})
        results_df[indicator] /= results_df["Area"]
    
    results_df = results_df.append(summary_dict, ignore_index = True)
    results_df.to_csv(filename + "/" + filename + "_county_outcome_summary.csv", header = True, index = None)
    

def process_results_for_management_decisions(filename):
    print("get land use management results for scenario: " + filename)
    
    cluid_fips_df = pd.read_csv("cluid_fips.csv")
    fips_list = cluid_fips_df["fips"].unique()
    
    results_df = pd.read_csv("scenarios.csv")
    for fip in fips_list:
        results_df[fip] = 0
        
    cluid_results_df = pd.read_csv(filename + "/" + filename + "_results.csv")
    cluid_results_df = pd.merge(cluid_results_df, cluid_fips_df, left_on = "CLU", right_on = "cluid")
    cluid_results_df = cluid_results_df[["fips", "Area", "Scenario1", "Share1", "Scenario2", "Share2", "Scenario3", "Share3"]]
    
    cluid_results_df.fillna(0, inplace = True)
    
    for i in range (1, 4):
        cluid_results_df["Share" + str(i)] = cluid_results_df["Share" + str(i)] * cluid_results_df["Area"]
    
    for index, row in results_df.iterrows():
        scenarioName = results_df.loc[index, "scenarios"]
        for fip in fips_list:
            temp_fip_df = cluid_results_df.loc[cluid_results_df["fips"] == fip]
            for i in range(1, 4):
                name = "Scenario" + str(i)
                share = "Share" + str(i)
                temp_df = temp_fip_df.loc[temp_fip_df[name] == scenarioName]
                if not temp_df.empty:
                    results_df.loc[index, fip] += temp_df[share].sum()
                    
    results_df.to_csv(filename + "/" + filename + "_land_management_by_county.csv", header = True, index = None)
        

#Example codes to caluclate for each scenario name

scenarios = ["baseline", "ave_dsoc_20p_0_2cost", "ave_no3_le_20p_ne_1_5cost", "ave_eros_20p_ne_45_6cost"]    

for scenario in scenarios:
    process_results_for_summary(scenario)
    process_results_for_management_decisions(scenario)
