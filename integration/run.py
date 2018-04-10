# -*- coding: utf-8 -*-

import csv
import os

from subprocess import check_output, CalledProcessError
from string import Template
from tempfile import TemporaryDirectory

import pandas as pd
import numpy as np

from smif.model.sector_model import SectorModel

class TransportWrapper(SectorModel):
    """Wrap the transport model
    """
    def initialise(self, initial_conditions):
        """Set up model state using initial conditions (any data required for
        the base year which would otherwise be output by a previous timestep) as
        necessary.
        """
        pass

    def simulate(self, data_handle):
        """Run the transport model

        Arguments
        ---------
        data_handle: smif.data_layer.DataHandle
        """
        src_dir = os.path.join(os.path.dirname(__file__), '..')
        with TemporaryDirectory() as temp_dir:
            self._set_parameters(data_handle, temp_dir)
            self._set_inputs(data_handle, temp_dir)
            self._run_model_subprocess(data_handle, src_dir, temp_dir)
            self._set_outputs(data_handle, temp_dir)

    def _run_model_subprocess(self, data_handle, src_dir, working_dir):
        path_to_jar = os.path.join(
            src_dir, 'transport', 'target',
            'transport-0.0.1-SNAPSHOT-jar-with-dependencies.jar'
        )

        path_to_config_template = os.path.join(
            src_dir, 'integration', 'template.properties'
        )

        path_to_config = os.path.join(
            working_dir, 'config.properties'
        )

        with open(path_to_config_template, 'r') as template_fh:
            config = Template(template_fh.read())

        config.substitute({
            'base_timestep': data_handle.base_timestep,
            'current_timestep': data_handle.current_timestep,
            'relative_path': os.path.relpath(working_dir)
        })

        with open(path_to_config, 'w') as template_fh:
            template_fh.write(config)

        self.logger.info("FROM run.py: Running transport model")
        arguments = [
            'java',
            '-cp',
            path_to_jar,
            'nismod.transport.App',
            path_to_config
        ]

        try:
            output = check_output(arguments)
            self.logger.debug(output)
        except CalledProcessError as ex:
            self.logger.exception("Transport model failed %s", ex)
            raise ex

    def _input_region_names(self, input_name):
        return self.inputs[input_name].spatial_resolution.get_entry_names()

    def _input_interval_names(self, input_name):
        return self.inputs[input_name].temporal_resolution.get_entry_names()

    def _set_parameters(self, data_handle, working_dir):
        """Read model parameters from data handle and set up config files
        """
        # Elasticities for passenger and freight demand
        variables = ['POPULATION', 'GVA', 'TIME', 'COST']
        types = {
            'ETA': os.path.join(working_dir, 'data', 'elasticities.csv'),
            'FREIGHT_ETA': os.path.join(working_dir, 'data', 'elasticitiesFreight.csv')
        }
        for suffix, filename in types.items():
            with open(filename, 'w') as file_handle:
                writer = csv.writer(file_handle)
                writer.writerow(('variable','elasticity'))
                for variable in variables:
                    key = "{}_{}".format(variable, suffix)
                    value = data_handle.get_parameter(key)
                    writer.writerow(variable, value)

    def _set_inputs(self, data_handle, working_dir):
        """Get model inputs from data handle and write to input files
        """
        # TODO with OA-level data
        # areaCodeFileName = nomisPopulation.csv
        # area_code,zone_code,population
        # OA        LAD       integer count of people

        # populationFile = population.csv
        # year,          E06000045,E07000086,E07000091,E06000046
        # base/current   [LAD,...]
        # 2015,          247000,129000,179000,139000
        # 2020,          247000,129000,179000,139000
        with open('population.csv' ,'w') as file_handle:
            w = csv.writer(file_handle)

            pop_region_names = self._input_region_names("population")
            w.writerow(('year', ) + tuple(pop_region_names))

            base_population = data_handle.get_base_timestep_data("population")[:,0]
            w.writerow((data_handle.base_timestep, ) + tuple(base_population))

            current_population = data_handle.get_data("population")[:,0]
            w.writerow((data_handle.current_timestep, ) + tuple(current_population))

        # TODO base and current gva_per_head
        # GVAFile = GVA.csv
        # year,          E06000045,E07000086,E07000091,E06000046
        # base/current   [LAD,...]
        # 2015,          23535.00,27860.00,24418.00,17739.00
        # 2020,          23535.00,27860.00,24418.00,17739.00

    def _set_outputs(self, data_handle, working_dir)::
        """Read results from model and write to data handle
        """
        # energyConsumptions.csv
        # year,PETROL,DIESEL,LPG,ELECTRICITY,HYDROGEN,HYBRID
        # 2020,11632.72,17596.62,2665.98,7435.64,94.57,714.32
        with open(os.path.join(working_dir, 'energyConsumptions.csv')) as fh:
            r = csv.reader(fh)
            header = next(r)[1:]
            values = next(r)[1:]
            for fuel, val in zip(header, values):
                data_handle.set_results(
                    "energy_consumption__{}".format(fuel.lower()),
                    np.array([[float(val)]])
                )

    def extract_obj(self, results):
        """Return value of objective function, to-be-defined
        """
        pass
