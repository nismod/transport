# -*- coding: utf-8 -*-

from smif.sector_model import SectorModel
from smif import SpaceTimeValue
from subprocess import check_output
import os
import csv

class TransportWrapper(SectorModel):
    """Wraps the transport model
    """
    base_data = {
        'year': 2015,
        'PETROL': 1.17,
        'DIESEL': 1.2,
        'LPG': 0.6,
        'ELECTRICITY': 0.1,
        'HYDROGEN': 4.19,
        'HYBRID': 1.17,
    }

    def simulate(self, decisions, state, data):
        """Runs the transport model

        Arguments
        ---------
        decisions : list
        state : list
        data : dict
        """
        this_path = os.path.dirname(os.path.realpath(__file__))
        data_file = "./transport/src/test/resources/testdata/energyUnitCosts.csv"
        path_to_data = os.path.join(this_path, data_file)

        with open(path_to_data, 'w') as fh:
            fieldnames = ['year', 'PETROL', 'DIESEL', 'LPG', 'ELECTRICITY', 'HYDROGEN', 'HYBRID']
            writer = csv.DictWriter(fh, fieldnames)
            writer.writeheader()

            price_set = {
                'year': data['timestep'],
                'PETROL': data['petrol_price'][0].value,
                'DIESEL': data['diesel_price'][0].value,
                'LPG': data['lpg_price'][0].value,
                'ELECTRICITY': data['electricity_price'][0].value,
                'HYDROGEN': data['hydrogen_price'][0].value,
                'HYBRID': data['hybrid_price'][0].value,
            }

            writer.writerow(TransportWrapper.base_data)
            writer.writerow(price_set)

        os.chdir(os.path.join(this_path, 'transport'))

        path_to_output = os.path.join(this_path, 'transport', 'energyConsumptions.csv')

        arguments = [
            'java',
            '-cp',
            './target/transport-0.0.1-SNAPSHOT-jar-with-dependencies.jar',
            'nismod.transport.App',
            str(TransportWrapper.base_data['year']),
            str(data['timestep']),
            path_to_data,
            path_to_output
        ]

        output = check_output(arguments)

        with open(path_to_output, 'r') as fh:
            reader = csv.DictReader(fh)
            output_data = next(reader)


        all_output = {
            'electricity_demand': [SpaceTimeValue('GB', '1', output_data['ELECTRICITY'], 'kWh')],
            'petrol_demand': [SpaceTimeValue('GB', '1', output_data['PETROL'], 'l')],
            'diesel_demand': [SpaceTimeValue('GB', '1', output_data['DIESEL'], 'l')],
            'lpg_demand': [SpaceTimeValue('GB', '1', output_data['LPG'], 'l')]
        }

        return all_output


    def extract_obj(self, results):
        return results

def main():

    data = {
        'timestep': 2016,
        'electricity_price': [SpaceTimeValue('GB', 'annual', '0.11', '£/kWh')],
        'petrol_price': [SpaceTimeValue('GB', 'annual', '1.18', '£/l')],
        'diesel_price': [SpaceTimeValue('GB', 'annual', '1.21', '£/l')],
        'LPG_price': [SpaceTimeValue('GB', 'annual', '0.61', '£/l')]
    }

    model = TransportWrapper()
    print("Running model")
    results = model.simulate([], [], data)
    print("Finished running model")

    print(results)

if __name__ == '__main__':

    main()

