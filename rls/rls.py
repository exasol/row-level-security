import json
import string
import sys
import glob

sys.path.extend(glob.glob('/buckets/bucketfs1/websocket_api/rsa-4.0-*.whl'))
sys.path.extend(glob.glob('/buckets/bucketfs1/websocket_api/websocket_client-*/*'))
sys.path.extend(glob.glob('/buckets/bucketfs1/websocket_api/exasol/*'))

import EXASOL


# datatype conversion

def get_datatype(name, maxsize, prec, scale):
    if name.startswith('VARCHAR'):
        return {'type': 'VARCHAR', 'size': maxsize}
    if name == 'TIMESTAMP':
        return {'type': 'TIMESTAMP'}
    if name == 'DATE':
        return {'type': 'DATE'}
    if name == 'DOUBLE':
        return {'type': 'DOUBLE'}
    if name.startswith('DECIMAL'):
        return {'type': 'DECIMAL', 'precision': prec, 'scale': scale}
    if name.startswith('CHAR'):
        return {'type': 'CHAR', 'size': maxsize}
    if name == 'BOOLEAN':
        return {'type': 'BOOLEAN'}
    raise ValueError("Datatype '" + name + "' yet not supported in RLS virtual schema")


# This function reads all the meta data for all the tables in the wrapped schema

def get_meta_for_schema(cn, s):
    c = exa.get_connection(cn)

    tabs = []
    with EXASOL.connect(c.address, c.user, c.password) as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                "select table_name from EXA_ALL_TABLES where table_schema='" + s + "' UNION ALL select view_name as table_name from EXA_ALL_VIEWS where view_schema='" + s + "';")

            for row in cursor:
                tabs.append(row[0])

        rtabs = []
        for t in tabs:
            with connection.cursor() as cursor:
                cursor.execute(
                    "select column_name, column_type, column_maxsize, column_num_prec, column_num_scale from EXA_ALL_COLUMNS where column_schema='"
                    + s + "' and column_table='" + t
                    + "' order by column_ordinal_position;")
                cols = []
                for row in cursor:
                    cols.append({'name': row[0],
                                 'dataType': get_datatype(row[1], row[2], row[3], row[4])})
                rtabs.append({'name': t, 'columns': cols})
        return rtabs


# This function reads all the meta data for selected tables in the wrapped schema
def get_meta_for_schema_with_names(cn, s, namelist):
    c = exa.get_connection(cn)

    tabs = []
    with EXASOL.connect(c.address, c.user, c.password) as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                "select table_name from EXA_ALL_TABLES where table_schema='" + s + "' UNION ALL select view_name as table_name from EXA_ALL_VIEWS where view_schema='" + s + "';")
            for row in cursor:
                if (row[0] in namelist):
                    tabs.append(row[0])

        rtabs = []
        for t in tabs:
            with connection.cursor() as cursor:
                cursor.execute(
                    "select column_name, column_type, column_maxsize, column_num_prec, column_num_scale from EXA_ALL_COLUMNS where column_schema='" + s + "' and column_table='" + t + "' order by column_ordinal_position;")
                cols = []
                for row in cursor:
                    cols.append({"name": row[0], "dataType": get_datatype(row[1], row[2], row[3], row[4])})
                rtabs.append({"name": t, "columns": cols})
        return rtabs


# load rls configuration into JSON for adapter notes
def get_rls_cls_config(cn, rls_c, rls_t, cls_t):
    # configuration dictionary
    config_dict = {}
    rls_config = {}
    cls_config = {}
    cls_schema = {}
    cls_table = {}
    config_dict['rls_config_table'] = rls_t
    config_dict['rls_column'] = rls_c
    config_dict['cls_config_table'] = cls_t

    c = exa.get_connection(cn)

    with EXASOL.connect(c.address, c.user, c.password) as connection:
        # read rls config table
        with connection.cursor() as cursor_rls:
            cursor_rls.execute(
                "select upper(user_name), group_concat(" + rls_c + ") rls_list from " + rls_t + " group by user_name;")

            for row in cursor_rls:
                v_user = row[0]
                v_rls_id = row[1]

                rls_config[v_user] = v_rls_id

            config_dict['rls_config'] = rls_config

        # read cls config table
        with connection.cursor() as cursor_cls:
            cursor_cls.execute(
                "select user_name, schema_name, table_name, group_concat(restricted_column) from " + cls_t + " group by user_name, schema_name, table_name order by user_name, schema_name, table_name;")

            # initiate v_user
            v_prev_user = ''
            v_prev_schema = ''

            # loop over result set
            for row in cursor_cls:

                v_user = str(row[0])
                v_schema = str(row[1])
                v_table = str(row[2])
                v_column = str(row[3])

                # reset lists, when new user occurs
                if v_user != v_prev_user and v_prev_user != '':
                    cls_table = {}
                    cls_schema = {}

                # reset list, when new schema occurs
                if v_schema != v_prev_schema and v_prev_schema != '':
                    cls_table = {}

                cls_table[v_table] = v_column
                cls_schema[v_schema] = cls_table
                cls_config[v_user] = cls_schema

                v_prev_user = v_user
                v_prev_schema = v_schema

        config_dict['cls_config'] = cls_config

    # convert to JSON string
    rls_cls_json = json.dumps(config_dict)

    # check JSON length
    if len(rls_cls_json) > 2000000:
        raise ValueError('RLS Config error: config JSON exeeds 2.000.000 characters')

    return rls_cls_json


# refresh rls cls configuration
def refresh_rls_cls_config(cn, notes):
    # read old config
    v_old_rls_cls_json = json.loads(notes)

    v_rls_cls_config_table = v_old_rls_cls_json["rls_config_table"]
    v_rls_cls_column = v_old_rls_cls_json["rls_column"]

    # re-run rls cls configuration
    v_rls_cls_json = get_rls_cls_config(cn, v_rls_cls_column, v_rls_cls_config_table, v_rls_cls_config_table)

    return v_rls_cls_json


# get rls filter for specific user
def get_rls_filter(username, notes):
    # read config
    v_rls_cls_json = json.loads(notes)
    # get rls config
    v_rls_json = v_rls_cls_json["rls_config"]

    # check if user exits in rls config
    if not username in v_rls_json:
        raise ValueError('RLS Config error: no RLS config for user')

    # build where clause for rls column filter
    v_rls_filter = v_rls_cls_json["rls_column"] + ' in (' + v_rls_json[username] + ')'

    return v_rls_filter


def check_cls(user_name, schema_name, table_name, column_name, column_data_type, notes):
    # read config
    v_rls_cls_json = json.loads(notes)
    # get cls config
    v_cls_json = v_rls_cls_json["cls_config"]

    # check if column is in restricted list of user exits
    try:

        for csl_col in v_cls_json[user_name][schema_name][table_name].split(","):

            if column_name == csl_col:
                # column is restricted -> only NULL as constant

                # cast NULL based on column data type size / precision
                if column_data_type['type'] == "VARCHAR":
                    return "cast(NULL as VARCHAR(" + str(column_data_type['size']) + ")) as " + column_name
                if column_data_type['type'] == 'CHAR':
                    return "cast(NULL as CHAR(" + str(column_data_type['size']) + ")) as " + column_name
                if column_data_type['type'] == 'DECIMAL':
                    return "cast(NULL as CHAR(" + str(column_data_type['precision']) + ',' + str(
                        column_data_type['scale']) + ")) as " + column_name

                # no cast needed
                return "(NULL as " + column_data_type + ") as " + column_name

        # column is not restricted -> column name will be returned
        return column_name

    except KeyError:
        # combination of user, schema, table is not part of the cls config
        # -> column is not restricted
        return column_name
    except:
        # all other errors are raised
        raise


# This function implements the virtual schema adapter callback

def adapter_call(request):
    # database expects utf-8 encoded string of type str. unicode not yet supported

    root = json.loads(request)

    # logic for creation of virtual schema
    if root['type'] == 'createVirtualSchema':
        if not 'properties' in root['schemaMetadataInfo']:
            raise ValueError('Config error: required properties: "TABLE_SCHEMA" and "META_CONNECTION" not given')
        if not 'TABLE_SCHEMA' in root['schemaMetadataInfo']['properties']:
            raise ValueError('Config error: required property "TABLE_SCHEMA" not given')
        if not 'META_CONNECTION' in root['schemaMetadataInfo']['properties']:
            raise ValueError('Config error: required property "META_CONNECTION" not given')
        if not 'RLS_COLUMN_NAME' in root['schemaMetadataInfo']['properties']:
            raise ValueError('Config error: required property "RLS_COLUMN_NAME" not given')
        if not 'RLS_CONFIG_TABLE' in root['schemaMetadataInfo']['properties']:
            raise ValueError('Config error: required property "RLS_CONFIG_TABLE" not given')
        if not 'CLS_CONFIG_TABLE' in root['schemaMetadataInfo']['properties']:
            raise ValueError('Config error: required property "CLS_CONFIG_TABLE" not given')

        sn = root['schemaMetadataInfo']['properties']['TABLE_SCHEMA']
        cn = root['schemaMetadataInfo']['properties']['META_CONNECTION']
        rls_c = root['schemaMetadataInfo']['properties']['RLS_COLUMN_NAME']
        rls_t = root['schemaMetadataInfo']['properties']['RLS_CONFIG_TABLE']
        cls_t = root['schemaMetadataInfo']['properties']['CLS_CONFIG_TABLE']

        res = {'type': 'createVirtualSchema',
               'schemaMetadata': {'adapterNotes': get_rls_cls_config(cn, rls_c, rls_t, cls_t),
                                  'tables': get_meta_for_schema(cn, sn)}}

        return json.dumps(res).encode('utf-8')

    # logic for drop of virtual schema
    elif root['type'] == 'dropVirtualSchema':
        return json.dumps({'type': 'dropVirtualSchema'}).encode('utf-8')

    elif root['type'] == 'setProperties':
        return json.dumps({'type': 'setProperties'}).encode('utf-8')

    elif root['type'] == 'refresh':
        sn = root["schemaMetadataInfo"]["properties"]["TABLE_SCHEMA"]
        cn = root["schemaMetadataInfo"]["properties"]["META_CONNECTION"]
        notes = root["schemaMetadataInfo"]["adapterNotes"]
        if "requestedTables" in root:
            namelist = root["requestedTables"]
            return json.dumps({"type": "refresh",
                               "schemaMetadata": {'adapterNotes': refresh_rls_cls_config(cn, notes),
                                                  "tables": get_meta_for_schema_with_names(cn, sn, namelist)}}).encode(
                'utf-8')
        else:
            return json.dumps({"type": "refresh",
                               "schemaMetadata": {'adapterNotes': refresh_rls_cls_config(cn, notes),
                                                  "tables": get_meta_for_schema(cn, sn)}}).encode('utf-8')

    if root["type"] == "getCapabilities":
        return json.dumps({
            "type": "getCapabilities",
            "capabilities": ["SELECTLIST_PROJECTION"]
        }).encode('utf-8')  # database expects utf-8 encoded string of type str. unicode not yet supported.
    elif root['type'] == 'pushdown':
        req = root['pushdownRequest']
        sn = root['schemaMetadataInfo']['properties']['TABLE_SCHEMA']
        notes = root["schemaMetadataInfo"]["adapterNotes"]

        if req['type'] != 'select':
            raise ValueError('Unsupported pushdown type: ' + req['type'])
        from_ = req['from']
        if from_['type'] != 'table':
            raise ValueError('Unsupported pushdown from: '
                             + from_['type'])
        table_ = from_['name']

        selectlist = ''

        if 'selectList' in req:
            # single columns in select list
            firstvalue = False

            for entry in req['selectList']:

                if firstvalue:
                    selectlist += ', '
                if entry['name'].upper() == 'FINAL':
                    selectlist += '"'
                    selectlist += entry['name'].upper()
                    selectlist += '"'
                else:
                    # get datatype of column
                    for col in root['involvedTables'][0]['columns']:
                        # search for data type in involved column list
                        if entry['name'].upper() == col['name']:
                            entry_datatype = col['dataType']

                    # check cls for column
                    selectlist += check_cls(exa.meta.current_user, sn, table_, entry['name'], entry_datatype, notes)
                firstvalue = True
        else:
            # no select list -> all columns selected or select *
            # all columns need to be determined by involved table info of adapter

            firstvalue = False

            # index 0 works, as a pushdown is generated for each table
            # if more than one table is part of the pushdown, this need to be adapted
            for col in root['involvedTables'][0]['columns']:

                if firstvalue:
                    selectlist += ', '
                if col['name'].upper() == 'FINAL':
                    selectlist += '"'
                    selectlist += col['name'].upper()
                    selectlist += '"'
                else:
                    # check cls for column
                    selectlist += check_cls(exa.meta.current_user, sn, table_, col['name'], col['dataType'], notes)
                firstvalue = True

        res = {'type': 'pushdown',
               'sql': 'SELECT ' + selectlist + ' FROM ' + sn + '.' + table_ + " WHERE " + get_rls_filter(
                   exa.meta.current_user, notes)}

        return json.dumps(res).encode('utf-8')
    else:
        raise ValueError('Unsupported callback')