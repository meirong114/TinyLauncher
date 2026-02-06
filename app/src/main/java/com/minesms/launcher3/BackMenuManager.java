package com.minesms.launcher3;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class BackMenuManager {
    private Context context;
    private List<MenuOption> menuOptions;
    private Dialog menuDialog;

    public BackMenuManager(Context context) {
        this.context = context;
        this.menuOptions = new ArrayList<>();
        initializeDefaultOptions();
    }

    private void initializeDefaultOptions() {
        // 默认的四个选项
        addOption("更换壁纸", R.drawable.ic_wallpaper, new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                    context.startActivity(Intent.createChooser(intent, "选择壁纸"));
                }
            });

        addOption("系统设置", R.drawable.ic_settings, new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(Settings.ACTION_SETTINGS);
                    context.startActivity(intent);
                }
            });

        addOption("桌面设置", R.drawable.ic_launcher_settings, new Runnable() {
                @Override
                public void run() {
                    Intent settingsIntent = new Intent(context, AppSettings.class);
                    context.startActivity(settingsIntent);
                }
            });

        addOption("侦错模式", R.drawable.ic_debug, new Runnable() {
                @Override
                public void run() {
                    // 显示调试信息对话框
                    showDebugDialog();
                }
            });
    }

    // 添加新选项的公共方法（方便后续扩展）
    public void addOption(String title, int iconResId, Runnable action) {
        menuOptions.add(new MenuOption(title, iconResId, action));
    }

    // 插入选项到指定位置
    public void insertOption(int position, String title, int iconResId, Runnable action) {
        if (position >= 0 && position <= menuOptions.size()) {
            menuOptions.add(position, new MenuOption(title, iconResId, action));
        }
    }

    // 移除选项
    public void removeOption(String title) {
        for (int i = 0; i < menuOptions.size(); i++) {
            if (menuOptions.get(i).getTitle().equals(title)) {
                menuOptions.remove(i);
                break;
            }
        }
    }

    // 显示菜单
    public void showMenu() {
        if (menuDialog != null && menuDialog.isShowing()) {
            menuDialog.dismiss();
        }

        menuDialog = new Dialog(context, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_back_menu, null);

        ListView listView = dialogView.findViewById(R.id.menu_listview);
        TextView titleText = dialogView.findViewById(R.id.menu_title);

        titleText.setText("功能菜单");

        // 创建适配器
        MenuAdapter adapter = new MenuAdapter(context, menuOptions);
        listView.setAdapter(adapter);

        // 设置列表项点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    MenuOption option = menuOptions.get(position);
                    option.getAction().run();
                    menuDialog.dismiss();
                }
            });

        // 设置对话框外部点击关闭
        menuDialog.setCancelable(true);
        menuDialog.setCanceledOnTouchOutside(true);
        menuDialog.setContentView(dialogView);
        menuDialog.show();
    }

    // 显示调试信息对话框
    private void showDebugDialog() {
        DebugDialog debugDialog = new DebugDialog(context);
        debugDialog.show();
    }

    // 自定义适配器
    private class MenuAdapter extends ArrayAdapter<MenuOption> {
        private LayoutInflater inflater;

        public MenuAdapter(Context context, List<MenuOption> options) {
            super(context, 0, options);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_menu_option, parent, false);
                holder = new ViewHolder();
                holder.icon = convertView.findViewById(R.id.option_icon);
                holder.title = convertView.findViewById(R.id.option_title);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            MenuOption option = getItem(position);
            if (option != null) {
                holder.icon.setImageResource(option.getIconResId());
                holder.title.setText(option.getTitle());
            }

            return convertView;
        }

        private class ViewHolder {
            ImageView icon;
            TextView title;
        }
    }
}

