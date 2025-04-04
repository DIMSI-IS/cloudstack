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
  <div>
    <h2>Dimsi !</h2>
  </div>
</template>

<script>

export default {
  name: 'Backroll',
  components: {
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
  .breadcrumb-card {
    margin-left: -24px;
    margin-right: -24px;
    margin-top: -16px;
    margin-bottom: 12px;
  }

  .chart-card-inner {
    text-align: center;
    white-space: nowrap;
    overflow: hidden;
  }
  .intermediate-certificate {
    opacity: 1;
    transform: none;
    transition: opacity 0.2s ease 0s, transform 0.5s ease;
    will-change: transform;
  }
  .intermediate-certificate.fadeInUp-enter-active {
    opacity: 0;
    transform: translateY(10px);
    transition: none;
  }
  .controls {
    display: flex;
    justify-content: flex-end;
  }
  .close-button {
    margin-right: 20px;
  }
  .ant-form-item {
    margin-bottom: 10px;
  }
</style>
