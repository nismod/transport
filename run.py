# -*- coding: utf-8 -*-

from smif.sector_model import SectorModel
from subprocess import check_output
import os
import csv

class TransportWrapper(SectorModel):
    """Wraps the transport model
    """
    base_data =  {
                'year': 2015,
                'PETROL':1.17,
                'DIESEL':1.2,
                'LPG':0.6,
                'ELECTRICITY':0.1
    }

    def simulate(self, decisions, state, data):
        """Runs the transport model

        Arguments
        ---------
        decisions : list
        state : list
        data : dict
        """
        with open("./transport/src/test/resources/testdata/energyUnitCosts.csv", 'w') as fh:
            fieldnames = ['year','PETROL','DIESEL','LPG', 'ELECTRICITY']
            writer = csv.DictWriter(fh, fieldnames)
            writer.writeheader()
   
            price_set = {
                    'year': data['year']['GB']['year']['value'],
                    'PETROL': data['petrol_price']['GB']['year']['value'],
                    'DIESEL': data['diesel_price']['GB']['year']['value'],
                    'LPG': data['LPG_price']['GB']['year']['value'],
                    'ELECTRICITY': data['electricity_price']['GB']['year']['value']
            }

            writer.writerow(TransportWrapper.base_data)
            writer.writerow(price_set)
            
        os.chdir('./transport')
        arguments = [
            'java',
            '-cp',
            './target/transport-0.0.1-SNAPSHOT-main.jar',
            'nismod.transport.App',
            str(TransportWrapper.base_data['year']),
            str(data['year']['GB']['year']['value']),
            './src/test/resources/testdata/energyUnitCosts.csv',
            './energyConsumptions.csv'
        ]

        output = check_output(arguments)

        with open("./energyConsumptions.csv", 'r') as fh:
            reader = csv.DictReader(fh)
            output_data = next(reader)


        all_output = {
            'electricity_demand': { 'GB': { '1': { 'value': output_data['ELECTRICITY'], 'units': 'kWh'}}},
            'petrol_demand': { 'GB': { '1': { 'value': output_data['PETROL'], 'units': 'l'}}},
            'diesel_demand': { 'GB': { '1': { 'value': output_data['DIESEL'], 'units': 'l'}}},
            'LPG_demand': { 'GB': { '1': { 'value': output_data['LPG'], 'units': 'l'}}}
            }
        
        return all_output


    def extract_obj(self, results):
        return results

if __name__ == '__main__':


    data = {
        'year': { 'GB': { 'year': { 'value': 2016, 'units': 'y' }}},   
        'electricity_price': { 'GB': { 'year': { 'value': 0.11, 'units': '£/kWh'}}},
        'petrol_price': { 'GB': { 'year': { 'value': 1.18, 'units': '£/l'}}},
        'diesel_price': { 'GB': { 'year': { 'value': 1.21, 'units': '£/l'}}},
        'LPG_price': { 'GB': { 'year': { 'value': 0.61, 'units': '£/l'}}}
    }

    model = TransportWrapper()
    print("Running model")
    results = model.simulate([], [], data)
    print("Finished running model")

    print(results)
