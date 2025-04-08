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
  <div>
    <chart-card class="filter-card">
      <template #title>
        <div class="filter-container">
          <div class="select-container">
            <a-select
              style="width: 120px">
              <a-select-option value="User">User</a-select-option>
              <a-select-option value="Account">Account</a-select-option>
              <a-select-option value="Domain">Domain</a-select-option>
              <a-select-option value="Project">Project</a-select-option>
            </a-select>
          </div>
          <div class="select-container">
            <a-select
              style="width: 120px">
              <a-select-option value="user">User</a-select-option>
              <a-select-option value="account">Account</a-select-option>
              <a-select-option value="domain">Domain</a-select-option>
              <a-select-option value="project">Project</a-select-option>
            </a-select>
          </div>
          <div class="radio-container">
            <a-radio-group button-style="solid">
              <a-radio-button value="daily">Daily</a-radio-button>
              <a-radio-button value="weekly">Weekly</a-radio-button>
              <a-radio-button value="monthly">Monthly</a-radio-button>
              <a-radio-button value="yearly">Yearly</a-radio-button>
            </a-radio-group>
          </div>
        </div>
      </template>
    </chart-card>
    <div class="chart-container">
      <chart-card class="chart-card-left">
          <template #title>
            <div class="center">
              <h3>
                <bank-outlined />
                CPU Speed
              </h3>
            </div>
          </template>
      </chart-card>
      <chart-card class="chart-card-right">
          <template #title>
            <div class="center">
              <h3>
                <bank-outlined />
                CPU Number
              </h3>
            </div>
          </template>
      </chart-card>
    </div>
    <div class="chart-container">
      <chart-card class="chart-card-left">
          <template #title>
            <div class="center">
              <h3>
                <bank-outlined />
                OS Repartition
              </h3>
            </div>
          </template>
      </chart-card>
      <chart-card class="chart-card-right">
          <template #title>
            <div class="center">
              <h3>
                <bank-outlined />
                RAM
              </h3>
            </div>
          </template>
      </chart-card>
    </div>
    <div class="chart-container">
      <chart-card class="chart-card-left">
          <template #title>
            <div class="center">
              <h3>
                <bank-outlined />
                Bandwith InOut
              </h3>
            </div>
          </template>
      </chart-card>
      <chart-card class="chart-card-right">
          <template #title>
            <div class="center">
              <h3>
                <bank-outlined />
                Storage
              </h3>
            </div>
          </template>
      </chart-card>
    </div>
    <div class="chart-container">
      <chart-card class="chart-card-left-little">
          <template #title>
            <div class="center">
              <h3>
                <bank-outlined />
                Nb VM actuel
              </h3>
            </div>
          </template>
      </chart-card>
      <chart-card class="chart-card-right-big">
          <template #title>
            <div class="center">
              <h3>
                <bank-outlined />
                Historique Nb VM
              </h3>
            </div>
          </template>
      </chart-card>
    </div>
    <div class="chart-container">
      <chart-card class="chart-card-left-big">
          <template #title>
            <div class="center">
              <h3>
                <bank-outlined />
                Historique Nb VR
              </h3>
            </div>
          </template>
      </chart-card>
      <chart-card class="chart-card-right-little">
          <template #title>
            <div class="center">
              <h3>
                <bank-outlined />
                Nb VR actuel
              </h3>
            </div>
          </template>
      </chart-card>
    </div>
  </div>
</template>

<script>

import Breadcrumb from '@/components/widgets/Breadcrumb'
import ChartCard from '@/components/widgets/ChartCard'
import ResourceLayout from '@/layouts/ResourceLayout'

export default {
  name: 'finops',
  components: {
    ChartCard,
    Breadcrumb,
    ResourceLayout
  },
  data () {
    return {
    }
  },
  beforeCreate () {
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
    min-height: 270px;
    margin-top: 20px;
  }

  .chart-container {
    display: inline-flex;
    width: 100%;
    min-height: 270px;
  }

  .chart-card-left {
    width: 50%;
    min-height: 270px;
    margin-top: 10px;
    margin-right: 10px;
  }

  .chart-card-left-big {
    width: 80%;
    min-height: 270px;
    margin-top: 10px;
    margin-right: 10px;
  }

  .chart-card-left-little {
    width: 20%;
    min-height: 270px;
    margin-top: 10px;
    margin-right: 10px;
  }

  .chart-card-right {
    width: 50%;
    min-height: 270px;
    margin-top: 10px;
    margin-left: 10px;
  }

  .chart-card-right-big {
    width: 80%;
    min-height: 270px;
    margin-top: 10px;
    margin-left: 10px;
  }

  .chart-card-right-little {
    width: 20%;
    min-height: 270px;
    margin-top: 10px;
    margin-left: 10px;
  }

  .filter-card {
    width: 100%;
    min-height: 80px;
    margin-bottom: 20px;
  }

  .breadcrumb-card {
    margin-left: -24px;
    margin-right: -24px;
    margin-top: -16px;
    margin-bottom: 12px;
  }

  .filter-container {
    display: inline-flex;
    width: 100%;
  }

  .select-container {
    margin-right: 20px;
  }

  .radio-container {
    align-items: right;
  }
</style>
