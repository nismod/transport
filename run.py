# -*- coding: utf-8 -*-

import csv
import os

from subprocess import check_output, CalledProcessError
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
        source_dir = os.path.realpath(os.path.dirname(__file__))
        with TemporaryDirectory() as temp_dir:
            pass

        self._set_parameters(data_handle, source_dir)
        self._set_inputs(data_handle, source_dir)
        self._run_model_subprocess(source_dir)
        self._set_outputs(data_handle, source_dir)

    def _run_model_subprocess(self, source_dir):
        path_to_java_project = os.path.join(
            source_dir,
            'transport'
        )

        path_to_config = os.path.join(
            path_to_java_project,
            'src', 'test', 'config', 'testConfig.properties'
        )

        path_to_jar = os.path.join(
            path_to_java_project,
            'target', 'transport-0.0.1-SNAPSHOT-jar-with-dependencies.jar'
        )

        # change directory to project (must restore after)
        dir_to_restore = os.getcwd()
        os.chdir(path_to_java_project)

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

        # change directory back to previous
        os.chdir(dir_to_restore)

    def _input_region_names(self, input_name):
        return self.inputs[input_name].spatial_resolution.get_entry_names()

    def _input_interval_names(self, input_name):
        return self.inputs[input_name].temporal_resolution.get_entry_names()

    def _set_parameters(self, data_handle, source_dir):
        """Read model parameters from data handle and set up config files
        """
        # Speed and capacity parameters
        parameter_FREE_FLOW_SPEED_M_ROAD = data_handle.get_parameter('FREE_FLOW_SPEED_M_ROAD')
        self.logger.debug("parameter_FREE_FLOW_SPEED_M_ROAD = %s", parameter_FREE_FLOW_SPEED_M_ROAD)
        parameter_FREE_FLOW_SPEED_A_ROAD = data_handle.get_parameter('FREE_FLOW_SPEED_A_ROAD')
        self.logger.debug("parameter_FREE_FLOW_SPEED_A_ROAD = %s", parameter_FREE_FLOW_SPEED_A_ROAD)
        parameter_AVERAGE_SPEED_FERRY = data_handle.get_parameter('AVERAGE_SPEED_FERRY')
        self.logger.debug("parameter_AVERAGE_SPEED_FERRY = %s", parameter_AVERAGE_SPEED_FERRY)

        parameter_AVERAGE_ACCESS_EGRESS_SPEED_CAR = data_handle.get_parameter('AVERAGE_ACCESS_EGRESS_SPEED_CAR')
        self.logger.debug("parameter_AVERAGE_ACCESS_EGRESS_SPEED_CAR = %s", parameter_AVERAGE_ACCESS_EGRESS_SPEED_CAR)
        parameter_AVERAGE_ACCESS_EGRESS_SPEED_FREIGHT = data_handle.get_parameter('AVERAGE_ACCESS_EGRESS_SPEED_FREIGHT')
        self.logger.debug("parameter_AVERAGE_ACCESS_EGRESS_SPEED_FREIGHT = %s", parameter_AVERAGE_ACCESS_EGRESS_SPEED_FREIGHT)

        parameter_MAXIMUM_CAPACITY_M_ROAD = data_handle.get_parameter('MAXIMUM_CAPACITY_M_ROAD')
        self.logger.debug("parameter_MAXIMUM_CAPACITY_M_ROAD = %s", parameter_MAXIMUM_CAPACITY_M_ROAD)
        parameter_MAXIMUM_CAPACITY_A_ROAD = data_handle.get_parameter('MAXIMUM_CAPACITY_A_ROAD')
        self.logger.debug("parameter_MAXIMUM_CAPACITY_A_ROAD = %s", parameter_MAXIMUM_CAPACITY_A_ROAD)

        parameter_NUMBER_OF_LANES_M_ROAD = data_handle.get_parameter('NUMBER_OF_LANES_M_ROAD')
        self.logger.debug("parameter_NUMBER_OF_LANES_M_ROAD = %s", parameter_NUMBER_OF_LANES_M_ROAD)
        parameter_NUMBER_OF_LANES_A_ROAD = data_handle.get_parameter('NUMBER_OF_LANES_A_ROAD')
        self.logger.debug("parameter_NUMBER_OF_LANES_A_ROAD = %s", parameter_NUMBER_OF_LANES_A_ROAD)

        # BPR function parameters
        parameter_ALPHA = data_handle.get_parameter('ALPHA')
        self.logger.debug("parameter_ALPHA = %s", parameter_ALPHA)
        parameter_BETA_M_ROAD = data_handle.get_parameter('BETA_M_ROAD')
        self.logger.debug("parameter_BETA_M_ROAD = %s", parameter_BETA_M_ROAD)
        parameter_BETA_A_ROAD = data_handle.get_parameter('BETA_A_ROAD')
        self.logger.debug("parameter_BETA_A_ROAD = %s", parameter_BETA_A_ROAD)

        parameter_AVERAGE_INTERSECTION_DELAY = data_handle.get_parameter('AVERAGE_INTERSECTION_DELAY')
        self.logger.debug("parameter_AVERAGE_INTERSECTION_DELAY = %s", parameter_AVERAGE_INTERSECTION_DELAY)

        parameter_INTERZONAL_TOP_NODES = data_handle.get_parameter('INTERZONAL_TOP_NODES')
        self.logger.debug("parameter_INTERZONAL_TOP_NODES = %s", parameter_INTERZONAL_TOP_NODES)

        # Flags
        parameter_FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT = data_handle.get_parameter('FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT')
        self.logger.debug("parameter_FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT = %s", parameter_FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT)
        parameter_FLAG_ASTAR_IF_EMPTY_ROUTE_SET = data_handle.get_parameter('FLAG_ASTAR_IF_EMPTY_ROUTE_SET')
        self.logger.debug("parameter_FLAG_ASTAR_IF_EMPTY_ROUTE_SET = %s", parameter_FLAG_ASTAR_IF_EMPTY_ROUTE_SET)

        # Route choice parameters
        parameter_TIME = data_handle.get_parameter('TIME')
        self.logger.debug("parameter_TIME = %s", parameter_TIME)
        parameter_LENGTH = data_handle.get_parameter('LENGTH')
        self.logger.debug("parameter_LENGTH = %s", parameter_LENGTH)
        parameter_COST = data_handle.get_parameter('COST')
        self.logger.debug("parameter_COST = %s", parameter_COST)
        parameter_INTERSECTIONS = data_handle.get_parameter('INTERSECTIONS')
        self.logger.debug("parameter_INTERSECTIONS = %s", parameter_INTERSECTIONS)

        parameter_ROUTE_LIMIT = data_handle.get_parameter('ROUTE_LIMIT')
        self.logger.debug("parameter_ROUTE_LIMIT = %s", parameter_ROUTE_LIMIT)
        parameter_GENERATION_LIMIT = data_handle.get_parameter('GENERATION_LIMIT')
        self.logger.debug("parameter_GENERATION_LIMIT = %s", parameter_GENERATION_LIMIT)
        parameter_INITIAL_ROUTE_CAPACITY = data_handle.get_parameter('INITIAL_ROUTE_CAPACITY')
        self.logger.debug("parameter_INITIAL_ROUTE_CAPACITY = %s", parameter_INITIAL_ROUTE_CAPACITY)
        parameter_FREIGHT_SCALING_FACTOR = data_handle.get_parameter('FREIGHT_SCALING_FACTOR')
        self.logger.debug("parameter_FREIGHT_SCALING_FACTOR = %s", parameter_FREIGHT_SCALING_FACTOR)

        parameter_LINK_TRAVEL_TIME_AVERAGING_WEIGHT = data_handle.get_parameter('LINK_TRAVEL_TIME_AVERAGING_WEIGHT')
        self.logger.debug("parameter_LINK_TRAVEL_TIME_AVERAGING_WEIGHT = %s", parameter_LINK_TRAVEL_TIME_AVERAGING_WEIGHT)

        parameter_ASSIGNMENT_ITERATIONS = data_handle.get_parameter('ASSIGNMENT_ITERATIONS')
        self.logger.debug("parameter_ASSIGNMENT_ITERATIONS = %s", parameter_ASSIGNMENT_ITERATIONS)
        parameter_PREDICTION_ITERATIONS = data_handle.get_parameter('PREDICTION_ITERATIONS')
        self.logger.debug("parameter_PREDICTION_ITERATIONS = %s", parameter_PREDICTION_ITERATIONS)

        parameter_SEED = data_handle.get_parameter('SEED')
        self.logger.debug("parameter_SEED = %s", parameter_SEED)

        # Elasticities for passenger demand
        parameter_POPULATION_ETA = data_handle.get_parameter('POPULATION_ETA')
        self.logger.debug("parameter_POPULATION_ETA = %s", parameter_POPULATION_ETA)
        parameter_GVA_ETA = data_handle.get_parameter('GVA_ETA')
        self.logger.debug("parameter_GVA_ETA = %s", parameter_GVA_ETA)
        parameter_TIME_ETA = data_handle.get_parameter('TIME_ETA')
        self.logger.debug("parameter_TIME_ETA = %s", parameter_TIME_ETA)
        parameter_COST_ETA = data_handle.get_parameter('COST_ETA')
        self.logger.debug("parameter_COST_ETA = %s", parameter_COST_ETA)

        # Elasticities for freight demand
        parameter_POPULATION_FREIGHT_ETA = data_handle.get_parameter('POPULATION_FREIGHT_ETA')
        self.logger.debug("parameter_POPULATION_FREIGHT_ETA = %s", parameter_POPULATION_FREIGHT_ETA)
        parameter_GVA_FREIGHT_ETA = data_handle.get_parameter('GVA_FREIGHT_ETA')
        self.logger.debug("parameter_GVA_FREIGHT_ETA = %s", parameter_GVA_FREIGHT_ETA)
        parameter_TIME_FREIGHT_ETA = data_handle.get_parameter('TIME_FREIGHT_ETA')
        self.logger.debug("parameter_TIME_FREIGHT_ETA = %s", parameter_TIME_FREIGHT_ETA)
        parameter_COST_FREIGHT_ETA = data_handle.get_parameter('COST_FREIGHT_ETA')
        self.logger.debug("parameter_COST_FREIGHT_ETA = %s", parameter_COST_FREIGHT_ETA)

        # WebTAG energy consumption parameters {A,B,C,D} for {
        #   CAR:PETROL,DIESEL,HYBRID,ELECTRICITY,LPG,HYDROGEN
        #   VAN:PETROL,DIESEL
        #   ARTIC:DIESEL
        #   RIGID:DIESEL
        # }

        # Parameter PCU for {CAR,VAN,RIGID,ARTIC,AV}

        # input_time_of_day_distribution = data_handle.get_data("time_of_day_distribution")
        # input_autonomous_vehicles_fraction = data_handle.get_data("autonomous_vehicles_fraction")
        # input_engine_type_fractions = data_handle.get_data("engine_type_fractions")

    def _set_inputs(self, data_handle, source_dir):
        """Get model inputs from data handle and write to input files
        """
        input_population = data_handle.get_data("population")
        input_GVA_per_capita = data_handle.get_data("GVA_per_capita")

    def _set_outputs(self, data_handle, source_dir):
        """Read results from model and write to data handle
        """
        # energyConsumptions.csv
        # year,PETROL,DIESEL,LPG,ELECTRICITY,HYDROGEN,HYBRID
        # 2020,11632.72,17596.62,2665.98,7435.64,94.57,714.32
        with open(os.path.join(source_dir, 'transport', 'energyConsumptions.csv')) as fh:
            r = csv.reader(fh)
            header = next(r)[1:]
            values = next(r)[1:]
            for fuel, val in zip(header, values):
                data_handle.set_results(
                    "energy_consumption_{}".format(fuel),
                    np.array([[float(val)]])
                )

    def extract_obj(self, results):
        """Return value of objective function, to-be-defined
        """
        pass


def od_array_to_matrix(data_array, od_region_names):
    """Convert origin-destination data from a smif data array (assuming a single
    interval) to a matrix form with origins as row headings, destinations as
    column headings.

    Parameters
    ----------
    data_array: numpy.ndarray
        Data array of dimensions [number of o-d pairs] x [single interval]
    od_region_names: list
        list of o-d names, in "{origin_id}-{destination_id}" format

    Returns
    -------
    data_matrix: pandas.DataFrame
        data frame suitable for conversion straight to_csv
    """
    df = pd.DataFrame({
        # assume OD regions are named "origin_id-destination_id"
        "origin": [name.split("-")[0] for name in od_region_names],
        "destination": [name.split("-")[1] for name in od_region_names],
        # assume scenario data has single interval per region
        # - i.e. array shape is like (300,1)
        "val": data_array[:, 0]
    })
    # pivot destinations to become column headers
    data_matrix = pd.pivot_table(df, values='val', index='origin', columns=['destination'])
    return data_matrix


def od_matrix_to_array(data_matrix, od_region_names):
    """Convert origin-destination data from matrix form (with origins as row
    headings, destinations as column headings) back to data array suitable
    for smif outputs.

    Parameters
    ----------
    data_matrix: pandas.DataFrame
        data frame suitable for conversion straight to_csv
    od_region_names: list
        list of o-d names, in "{origin_id}-{destination_id}" format

    Returns
    -------
    data_array: numpy.ndarray
        Data array of dimensions [number of o-d pairs] x [single interval]
    """
    df = pd.melt(data_matrix, id_vars=['origin'])  # unpivot, retaining origin column
    df['name'] = df['origin'] + "-" + df['variable']  # concatenate region name strings
    del df['origin']
    del df['variable']
    ixd = df.set_index('name')  # create index on name
    s = ixd.loc[od_region_names, 'value']  # lookup values using region names in smif order
    n = s.as_matrix()  # convert series to ndarray
    data_array = n.reshape(n.size,1)  # reshape to nested array for smif
    return data_array
