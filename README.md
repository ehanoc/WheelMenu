# WheelMenu
A Rotating Menu Wheel

## How
- Clone
- Make module
- ```implementation project(':WheelMenu')```
- Include in your layout ``` @layout/wheel_menu_layout ```

## Configure @layout/wheel_menu_layout

```
<com.ehanoc.menuwheel.view.WheelMenuLayout
(...) 


app:dividers="3" (Number of sections in the menu)
>

    <com.ehanoc.menuwheel.view.CircleLayout
        android:id="@+id/circle_layout_id"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_gravity="center"
        android:rotation="0">


    (Menu items go where. Same number as in app:dividers above)


    </com.ehanoc.menuwheel.view.CircleLayout>
<com.ehanoc.menuwheel.view.WheelMenuLayout />
```

- TODO:: Merge WheelMenuLayout & CircleLayout into a single custom view

<img src="https://github.com/ehanoc/WheelMenu/blob/master/wheelmenu_gif.gif" width="300" height="600" />
