package org.nsdev.apps.linktester.inventory;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.nsdev.apps.linktester.R;

import java.util.HashMap;

/**
 * Created by neal on 2013-10-20.
 */
public class InventoryFragment extends Fragment {

    public interface InventoryFragmentProvider {
        WeaponTotals getWeaponTotals();

        InventoryTotal getInventoryTotal();

        ModTotals getModTotals();

        ResonatorTotals getResonatorTotals();

        PowerCubeTotals getPowerCubeTotals();

        int getPortalKeyCount();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.inventory_table, container);
    }

    public void updateFragment() {
        InventoryFragmentProvider provider = (InventoryFragmentProvider) getActivity();

        updateWeaponTotals(provider.getWeaponTotals());
        updateInventoryTotal(provider.getInventoryTotal());
        updateModTotals(provider.getModTotals());
        updateResonatorTotals(provider.getResonatorTotals());
        updatePortalKeyCount(provider.getPortalKeyCount());
        updatePowerCubeTotals(provider.getPowerCubeTotals());
    }

    int[] cubeColumnIds = {R.id.l1_cube_count, R.id.l2_cube_count, R.id.l3_cube_count, R.id.l4_cube_count, R.id.l5_cube_count, R.id.l6_cube_count, R.id.l7_cube_count, R.id.l8_cube_count};

    private void updatePowerCubeTotals(PowerCubeTotals powerCubeTotals) {

        for (int i = 1; i <= 8; i++) {
            int id = cubeColumnIds[i - 1];

            TextView count = (TextView) getView().findViewById(id);
            if (powerCubeTotals.getPowerCubes().containsKey(i)) {
                count.setText(String.valueOf(powerCubeTotals.getPowerCubes().get(i)));
            } else {
                count.setText("0");
            }
        }

    }

    private void updatePortalKeyCount(int portalKeyCount) {
        TextView count = (TextView) getView().findViewById(R.id.portal_key_count);
        count.setText(String.valueOf(portalKeyCount));
    }

    private void updateModTotals(ModTotals modTotals) {

        // Set up the row lookup table
        HashMap<String, View> rows = new HashMap<String, View>();

        rows.put("Multi-hack", getView().findViewById(R.id.mod_multi_hack_row));
        rows.put("Heat Sink", getView().findViewById(R.id.mod_heat_sink_row));
        rows.put("Portal Shield", getView().findViewById(R.id.mod_portal_shield_row));
        rows.put("Force Amp", getView().findViewById(R.id.mod_force_amp_row));
        rows.put("Turret", getView().findViewById(R.id.mod_turret_row));
        rows.put("Link Amp", getView().findViewById(R.id.mod_link_amp_row));

        // Set everything to zeros
        for (String key : rows.keySet()) {
            View v = rows.get(key);
            TextView c = (TextView) v.findViewById(R.id.common_count);
            TextView r = (TextView) v.findViewById(R.id.rare_count);
            TextView vr = (TextView) v.findViewById(R.id.very_rare_count);
            TextView name = (TextView) v.findViewById(R.id.mod_name);

            c.setText("0");
            r.setText("0");
            vr.setText("0");
            name.setText(key);
        }

        // Process common
        processModTotals(modTotals, rows, "COMMON", R.id.common_count);
        processModTotals(modTotals, rows, "RARE", R.id.rare_count);
        processModTotals(modTotals, rows, "VERY_RARE", R.id.very_rare_count);

    }

    private void processModTotals(ModTotals modTotals, HashMap<String, View> rows, String rarity, int columnId) {
        if (modTotals.getMods().containsKey(rarity)) {
            final HashMap<String, Integer> stringIntegerHashMap = modTotals.getMods().get(rarity);
            for (String key : stringIntegerHashMap.keySet()) {
                View v = rows.get(key);
                TextView t = (TextView) v.findViewById(columnId);
                t.setText(String.valueOf(stringIntegerHashMap.get(key)));
            }
        }
    }

    private void updateInventoryTotal(InventoryTotal inventoryTotal) {
        TextView count = (TextView) getView().findViewById(R.id.total_inventory_count);
        count.setText(String.valueOf(inventoryTotal.getTotalInventoryCount()));
    }

    int[] resonatorColumnIds = {R.id.l1_resonator_count, R.id.l2_resonator_count, R.id.l3_resonator_count, R.id.l4_resonator_count, R.id.l5_resonator_count, R.id.l6_resonator_count, R.id.l7_resonator_count, R.id.l8_resonator_count};

    private void updateResonatorTotals(ResonatorTotals resonatorTotals) {

        for (int i = 1; i <= 8; i++) {
            int id = resonatorColumnIds[i - 1];

            TextView count = (TextView) getView().findViewById(id);
            if (resonatorTotals.getResonators().containsKey(i)) {
                count.setText(String.valueOf(resonatorTotals.getResonators().get(i)));
            } else {
                count.setText("0");
            }
        }
    }

    int[] weaponColumnIds = {R.id.l1_burster_count, R.id.l2_burster_count, R.id.l3_burster_count, R.id.l4_burster_count, R.id.l5_burster_count, R.id.l6_burster_count, R.id.l7_burster_count, R.id.l8_burster_count};

    private void updateWeaponTotals(WeaponTotals weaponTotals) {

        for (int i = 1; i <= 8; i++) {
            int id = weaponColumnIds[i - 1];

            TextView count = (TextView) getView().findViewById(id);
            if (weaponTotals.getEmpWeapons().containsKey(i)) {
                count.setText(String.valueOf(weaponTotals.getEmpWeapons().get(i)));
            } else {
                count.setText("0");
            }
        }

        TextView count = (TextView) getView().findViewById(R.id.jarvis_count);
        count.setText("0");

        if (weaponTotals.getFlipCards().containsKey("JARVIS")) {
            count.setText(String.valueOf(weaponTotals.getFlipCards().get("JARVIS")));
        }

        count = (TextView) getView().findViewById(R.id.ada_count);
        count.setText("0");

        if (weaponTotals.getFlipCards().containsKey("ADA")) {
            count.setText(String.valueOf(weaponTotals.getFlipCards().get("ADA")));
        }

    }
}
