// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

<template>
  <a-row :gutter="12">
      <a-col :md="24">
      <a-card class="breadcrumb-card">
        <a-col :md="24" style="display: flex">
          <breadcrumb style="padding-top: 6px; padding-left: 8px" />
          <a-button
            style="margin-left: 12px; margin-top: 4px"
            :loading="loading"
            size="small"
            shape="round"
            @click="fetchData()" >
            <template #icon><ReloadOutlined /></template>
            {{ $t('label.refresh') }}
          </a-button>
        </a-col>
      </a-card>
    </a-col>
  </a-row>
  <div class="chart-container">
    <div class="little-card">
      <chart-card class="metric-card">
        <div class="center">
          <a-statistic title="Average Age Of Machines" :value="2.73" >
          <template #suffix>
            Years
            </template>
          </a-statistic>
        </div>
      </chart-card>
      <chart-card class="metric-card">
        <div class="center">
          <a-statistic title="Metrics 2" :value="93" >
          <template #suffix>
            <DesktopOutlined />
            </template>
          </a-statistic>
        </div>
      </chart-card>
    </div>
    <div class="big-card">
      <chart-card class="map">
        <iframe width="100%" height="730px" src="https://www.openstreetmap.org/export/embed.html?bbox=-125.15625000000001%2C-30.60009387355006%2C106.87500000000001%2C76.96033358827414&amp;layer=mapnik" style="border: 1px solid black"></iframe><br/><small><a href="https://www.openstreetmap.org/?#map=3/41.77/-9.14">Afficher une carte plus grande</a></small>
      </chart-card>
    </div>
  </div>
</template>

<script>

import ResourceLayout from '@/layouts/ResourceLayout'
import ChartCard from '@/components/widgets/ChartCard'

export default {
  name: 'greenops',
  components: {
    ResourceLayout,
    ChartCard
  },
  data () {
    return {
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('uploadCustomCertificate')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
    },
    fetchData () {
      this.routes = {}
    }
  }
}
</script>

<style lang="scss" scoped>

  .dashboard-card {
    width: 100%;
    min-height: 370px;
  }

  .breadcrumb-card {
    margin-left: -24px;
    margin-right: -24px;
    margin-top: -16px;
    margin-bottom: 12px;
  }

  .chart-container {
    display: inline-flex;
    width: 100%;
    min-height: 500px;
  }

  .little-card {
    width: 20%;
    min-height: 500px;
    margin-right: 10px;
  }

  .metric-card {
    width: 100%;
    min-height: 400px;
    margin-bottom: 10px;
  }

  .big-card {
    width: 80%;
    min-height: 810px;
  }

  .map {
    width: 100%;
    min-height: 810px;
  }
</style>
