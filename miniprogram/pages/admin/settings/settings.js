const api = require('../../../utils/request');

Page({
  data: {
    form: {
      shop_name: '', shop_notice: '', community_name: '', service_phone: '',
      delivery_slots_text: '', points_rate: '', level_silver: '', level_gold: '', level_black: ''
    },
    saving: false
  },

  onShow() {
    this.load();
  },

  load() {
    api.get('/api/admin/ops/settings').then((s) => {
      this.setData({
        form: {
          shop_name: s.shop_name || '',
          shop_notice: s.shop_notice || '',
          community_name: s.community_name || '',
          service_phone: s.service_phone || '',
          delivery_slots_text: (s.delivery_slots || '').replace(/[\[\]"]/g, '').replace(/,/g, '\n'),
          points_rate: s.points_rate || '1',
          level_silver: s.level_silver || '300',
          level_gold: s.level_gold || '1000',
          level_black: s.level_black || '3000'
        }
      });
    }).catch(() => {});
  },

  onField(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value });
  },

  save() {
    const f = this.data.form;
    const slots = f.delivery_slots_text.split(/[\n,，]/).map((s) => s.trim()).filter(Boolean);
    if (!slots.length) {
      return wx.showToast({ title: '请至少填写一个派送时段', icon: 'none' });
    }
    const payload = {
      shop_name: f.shop_name,
      shop_notice: f.shop_notice,
      community_name: f.community_name,
      service_phone: f.service_phone,
      delivery_slots: JSON.stringify(slots),
      points_rate: f.points_rate,
      level_silver: f.level_silver,
      level_gold: f.level_gold,
      level_black: f.level_black
    };
    this.setData({ saving: true });
    api.put('/api/admin/ops/settings', payload).then(() => {
      wx.showToast({ title: '已保存', icon: 'none' });
      this.setData({ saving: false });
    }).catch(() => this.setData({ saving: false }));
  }
});
