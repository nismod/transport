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
        # Get model parameters
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

        parameter_FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT = data_handle.get_parameter('FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT')
        self.logger.debug("parameter_FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT = %s", parameter_FLAG_INTRAZONAL_ASSIGNMENT_REPLACEMENT)
        parameter_FLAG_ASTAR_IF_EMPTY_ROUTE_SET = data_handle.get_parameter('FLAG_ASTAR_IF_EMPTY_ROUTE_SET')
        self.logger.debug("parameter_FLAG_ASTAR_IF_EMPTY_ROUTE_SET = %s", parameter_FLAG_ASTAR_IF_EMPTY_ROUTE_SET)

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

        parameter_POPULATION_ETA = data_handle.get_parameter('POPULATION_ETA')
        self.logger.debug("parameter_POPULATION_ETA = %s", parameter_POPULATION_ETA)
        parameter_GVA_ETA = data_handle.get_parameter('GVA_ETA')
        self.logger.debug("parameter_GVA_ETA = %s", parameter_GVA_ETA)
        parameter_TIME_ETA = data_handle.get_parameter('TIME_ETA')
        self.logger.debug("parameter_TIME_ETA = %s", parameter_TIME_ETA)
        parameter_COST_ETA = data_handle.get_parameter('COST_ETA')
        self.logger.debug("parameter_COST_ETA = %s", parameter_COST_ETA)

        parameter_POPULATION_FREIGHT_ETA = data_handle.get_parameter('POPULATION_FREIGHT_ETA')
        self.logger.debug("parameter_POPULATION_FREIGHT_ETA = %s", parameter_POPULATION_FREIGHT_ETA)
        parameter_GVA_FREIGHT_ETA = data_handle.get_parameter('GVA_FREIGHT_ETA')
        self.logger.debug("parameter_GVA_FREIGHT_ETA = %s", parameter_GVA_FREIGHT_ETA)
        parameter_TIME_FREIGHT_ETA = data_handle.get_parameter('TIME_FREIGHT_ETA')
        self.logger.debug("parameter_TIME_FREIGHT_ETA = %s", parameter_TIME_FREIGHT_ETA)
        parameter_COST_FREIGHT_ETA = data_handle.get_parameter('COST_FREIGHT_ETA')
        self.logger.debug("parameter_COST_FREIGHT_ETA = %s", parameter_COST_FREIGHT_ETA)

        parameter_CARPETROLA = data_handle.get_parameter('CAR|PETROL|A')
        self.logger.debug("parameter_CARPETROLA = %s", parameter_CARPETROLA)
        parameter_CARPETROLB = data_handle.get_parameter('CAR|PETROL|B')
        self.logger.debug("parameter_CARPETROLB = %s", parameter_CARPETROLB)
        parameter_CARPETROLC = data_handle.get_parameter('CAR|PETROL|C')
        self.logger.debug("parameter_CARPETROLC = %s", parameter_CARPETROLC)
        parameter_CARPETROLD = data_handle.get_parameter('CAR|PETROL|D')
        self.logger.debug("parameter_CARPETROLD = %s", parameter_CARPETROLD)

        parameter_CARDIESELA = data_handle.get_parameter('CAR|DIESEL|A')
        self.logger.debug("parameter_CARDIESELA = %s", parameter_CARDIESELA)
        parameter_CARDIESELB = data_handle.get_parameter('CAR|DIESEL|B')
        self.logger.debug("parameter_CARDIESELB = %s", parameter_CARDIESELB)
        parameter_CARDIESELC = data_handle.get_parameter('CAR|DIESEL|C')
        self.logger.debug("parameter_CARDIESELC = %s", parameter_CARDIESELC)
        parameter_CARDIESELD = data_handle.get_parameter('CAR|DIESEL|D')
        self.logger.debug("parameter_CARDIESELD = %s", parameter_CARDIESELD)

        parameter_VANPETROLA = data_handle.get_parameter('VAN|PETROL|A')
        self.logger.debug("parameter_VANPETROLA = %s", parameter_VANPETROLA)
        parameter_VANPETROLB = data_handle.get_parameter('VAN|PETROL|B')
        self.logger.debug("parameter_VANPETROLB = %s", parameter_VANPETROLB)
        parameter_VANPETROLC = data_handle.get_parameter('VAN|PETROL|C')
        self.logger.debug("parameter_VANPETROLC = %s", parameter_VANPETROLC)
        parameter_VANPETROLD = data_handle.get_parameter('VAN|PETROL|D')
        self.logger.debug("parameter_VANPETROLD = %s", parameter_VANPETROLD)

        parameter_VANDIESELA = data_handle.get_parameter('VAN|DIESEL|A')
        self.logger.debug("parameter_VANDIESELA = %s", parameter_VANDIESELA)
        parameter_VANDIESELB = data_handle.get_parameter('VAN|DIESEL|B')
        self.logger.debug("parameter_VANDIESELB = %s", parameter_VANDIESELB)
        parameter_VANDIESELC = data_handle.get_parameter('VAN|DIESEL|C')
        self.logger.debug("parameter_VANDIESELC = %s", parameter_VANDIESELC)
        parameter_VANDIESELD = data_handle.get_parameter('VAN|DIESEL|D')
        self.logger.debug("parameter_VANDIESELD = %s", parameter_VANDIESELD)

        parameter_RIGIDDIESELA = data_handle.get_parameter('RIGID|DIESEL|A')
        self.logger.debug("parameter_RIGIDDIESELA = %s", parameter_RIGIDDIESELA)
        parameter_RIGIDDIESELB = data_handle.get_parameter('RIGID|DIESEL|B')
        self.logger.debug("parameter_RIGIDDIESELB = %s", parameter_RIGIDDIESELB)
        parameter_RIGIDDIESELC = data_handle.get_parameter('RIGID|DIESEL|C')
        self.logger.debug("parameter_RIGIDDIESELC = %s", parameter_RIGIDDIESELC)
        parameter_RIGIDDIESELD = data_handle.get_parameter('RIGID|DIESEL|D')
        self.logger.debug("parameter_RIGIDDIESELD = %s", parameter_RIGIDDIESELD)

        parameter_ARTICDIESELA = data_handle.get_parameter('ARTIC|DIESEL|A')
        self.logger.debug("parameter_ARTICDIESELA = %s", parameter_ARTICDIESELA)
        parameter_ARTICDIESELB = data_handle.get_parameter('ARTIC|DIESEL|B')
        self.logger.debug("parameter_ARTICDIESELB = %s", parameter_ARTICDIESELB)
        parameter_ARTICDIESELC = data_handle.get_parameter('ARTIC|DIESEL|C')
        self.logger.debug("parameter_ARTICDIESELC = %s", parameter_ARTICDIESELC)
        parameter_ARTICDIESELD = data_handle.get_parameter('ARTIC|DIESEL|D')
        self.logger.debug("parameter_ARTICDIESELD = %s", parameter_ARTICDIESELD)

        parameter_CARELECTRICITYA = data_handle.get_parameter('CAR|ELECTRICITY|A')
        self.logger.debug("parameter_CARELECTRICITYA = %s", parameter_CARELECTRICITYA)
        parameter_CARELECTRICITYB = data_handle.get_parameter('CAR|ELECTRICITY|B')
        self.logger.debug("parameter_CARELECTRICITYB = %s", parameter_CARELECTRICITYB)
        parameter_CARELECTRICITYC = data_handle.get_parameter('CAR|ELECTRICITY|C')
        self.logger.debug("parameter_CARELECTRICITYC = %s", parameter_CARELECTRICITYC)
        parameter_CARELECTRICITYD = data_handle.get_parameter('CAR|ELECTRICITY|D')
        self.logger.debug("parameter_CARELECTRICITYD = %s", parameter_CARELECTRICITYD)

        parameter_CARLPGA = data_handle.get_parameter('CAR|LPG|A')
        self.logger.debug("parameter_CARLPGA = %s", parameter_CARLPGA)
        parameter_CARLPGB = data_handle.get_parameter('CAR|LPG|B')
        self.logger.debug("parameter_CARLPGB = %s", parameter_CARLPGB)
        parameter_CARLPGC = data_handle.get_parameter('CAR|LPG|C')
        self.logger.debug("parameter_CARLPGC = %s", parameter_CARLPGC)
        parameter_CARLPGD = data_handle.get_parameter('CAR|LPG|D')
        self.logger.debug("parameter_CARLPGD = %s", parameter_CARLPGD)

        parameter_CARHYDROGENA = data_handle.get_parameter('CAR|HYDROGEN|A')
        self.logger.debug("parameter_CARHYDROGENA = %s", parameter_CARHYDROGENA)
        parameter_CARHYDROGENB = data_handle.get_parameter('CAR|HYDROGEN|B')
        self.logger.debug("parameter_CARHYDROGENB = %s", parameter_CARHYDROGENB)
        parameter_CARHYDROGENC = data_handle.get_parameter('CAR|HYDROGEN|C')
        self.logger.debug("parameter_CARHYDROGENC = %s", parameter_CARHYDROGENC)
        parameter_CARHYDROGEND = data_handle.get_parameter('CAR|HYDROGEN|D')
        self.logger.debug("parameter_CARHYDROGEND = %s", parameter_CARHYDROGEND)

        parameter_CARHYBRIDA = data_handle.get_parameter('CAR|HYBRID|A')
        self.logger.debug("parameter_CARHYBRIDA = %s", parameter_CARHYBRIDA)
        parameter_CARHYBRIDB = data_handle.get_parameter('CAR|HYBRID|B')
        self.logger.debug("parameter_CARHYBRIDB = %s", parameter_CARHYBRIDB)
        parameter_CARHYBRIDC = data_handle.get_parameter('CAR|HYBRID|C')
        self.logger.debug("parameter_CARHYBRIDC = %s", parameter_CARHYBRIDC)
        parameter_CARHYBRIDD = data_handle.get_parameter('CAR|HYBRID|D')
        self.logger.debug("parameter_CARHYBRIDD = %s", parameter_CARHYBRIDD)

        parameter_CAR_PCU = data_handle.get_parameter('CAR_PCU')
        self.logger.debug("parameter_CAR_PCU = %s", parameter_CAR_PCU)
        parameter_VAN_PCU = data_handle.get_parameter('VAN_PCU')
        self.logger.debug("parameter_VAN_PCU = %s", parameter_VAN_PCU)
        parameter_RIGID_PCU = data_handle.get_parameter('RIGID_PCU')
        self.logger.debug("parameter_RIGID_PCU = %s", parameter_RIGID_PCU)
        parameter_ARTIC_PCU = data_handle.get_parameter('ARTIC_PCU')
        self.logger.debug("parameter_ARTIC_PCU = %s", parameter_ARTIC_PCU)
        parameter_AV_PCU = data_handle.get_parameter('AV_PCU')
        self.logger.debug("parameter_AV_PCU = %s", parameter_AV_PCU)

    def _set_inputs(self, data_handle, source_dir):
        # Get model inputs
        od_region_names = self._input_region_names("base_year_passenger_vehicle_OD_matrix")

        base_year_passenger_vehicle_OD_matrix = od_array_to_matrix(
            data_handle.get_base_timestep_data("base_year_passenger_vehicle_OD_matrix"),
            od_region_names
        )
        base_year_passenger_vehicle_OD_matrix.to_csv('passengerODM.csv')

        # no data yet for freight matrix (minitest)
        # input_base_year_freight_vehicle_OD_matrix = od_array_to_matrix(
        #     data_handle.get_data("base_year_freight_vehicle_OD_matrix"),
        #     od_region_names
        # )
        # input_base_year_freight_vehicle_OD_matrix.to_csv('freightODM.csv')

        # input_population = data_handle.get_data("population")
        # input_population_in_census_output_areas = data_handle.get_data("population_in_census_output_areas")
        # input_population_in_workplace_zones = data_handle.get_data("population_in_workplace_zones")
        # input_GVA_per_capita = data_handle.get_data("GVA_per_capita")

        # input_output_area_to_network_node_mapping = data_handle.get_data("output_area_to_network_node_mapping")
        # input_workplace_zone_to_network_node_mapping = data_handle.get_data("workplace_zone_to_network_node_mapping")
        # input_freight_zone_to_network_node_mapping = data_handle.get_data("freight_zone_to_network_node_mapping")
        # input_freight_zone_to_lad_mapping = data_handle.get_data("freight_zone_to_lad_mapping")

        # input_time_of_day_distribution = data_handle.get_data("time_of_day_distribution")
        # input_autonomous_vehicles_fraction = data_handle.get_data("autonomous_vehicles_fraction")
        # input_energy_unit_costs = data_handle.get_data("energy_unit_costs")
        # input_engine_type_fractions = data_handle.get_data("engine_type_fractions")

        # input_passenger_route_set = data_handle.get_data("passenger_route_set")
        # input_freight_route_set = data_handle.get_data("freight_route_set")

    def _set_outputs(self, data_handle, source_dir):
        # Read results from model and write to data handler

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

        # data_handle.set_results("vehicle_kilometres", None)

        # data_handle.set_results("link_travel_time", None)
        # data_handle.set_results("link_travel_cost", None)
        # data_handle.set_results("link_travel_speed", None)

        # data_handle.set_results("traffic_volume", None)
        # data_handle.set_results("traffic_flow", None)
        # data_handle.set_results("capacity_utilisation", None)

        # data_handle.set_results("link_travel_time_daily_average", None)
        # data_handle.set_results("link_travel_cost_daily_average", None)
        # data_handle.set_results("link_travel_speed_daily_average", None)

        # data_handle.set_results("traffic_volume_daily_total", None)
        # data_handle.set_results("traffic_flow_daily_average", None)

        # data_handle.set_results("passenger_vehicle_demand", None)
        # data_handle.set_results("freight_vehicle_demand", None)

        # data_handle.set_results("inter-zonal_travel_time", None)
        # data_handle.set_results("inter-zonal_travel_cost", None)
        # data_handle.set_results("inter-zonal_travel_time_daily_average", None)
        # data_handle.set_results("inter-zonal_travel_cost_daily_average", None)

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
